package pt.ua.tqs.tuttibox.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.ua.tqs.tuttibox.config.TuttiboxProperties;
import pt.ua.tqs.tuttibox.domain.CompartmentState;
import pt.ua.tqs.tuttibox.domain.Size;
import pt.ua.tqs.tuttibox.exception.ConflictException;
import pt.ua.tqs.tuttibox.exception.NotFoundException;
import pt.ua.tqs.tuttibox.model.Compartment;
import pt.ua.tqs.tuttibox.port.LockerHardwarePort;
import pt.ua.tqs.tuttibox.port.Notification;
import pt.ua.tqs.tuttibox.port.NotificationPort;
import pt.ua.tqs.tuttibox.repository.CompartmentRepository;

import java.util.List;
import java.util.Optional;

/** Compartment allocation, out-of-service rules and every call to the hardware port. */
@Service
public class CompartmentService {

    private static final Logger log = LoggerFactory.getLogger(CompartmentService.class);

    private final CompartmentRepository compartments;
    private final LockerHardwarePort hardware;
    private final NotificationPort notifier;
    private final TuttiboxProperties properties;

    public CompartmentService(CompartmentRepository compartments, LockerHardwarePort hardware,
                              NotificationPort notifier, TuttiboxProperties properties) {
        this.compartments = compartments;
        this.hardware = hardware;
        this.notifier = notifier;
        this.properties = properties;
    }

    /**
     * Reserves a free compatible compartment: same size first, then larger (if allowed), never smaller.
     * Must run inside the caller's transaction (the row lock lasts until commit).
     */
    @Transactional
    public Optional<Compartment> allocate(Long stationId, Size parcelSize) {
        for (Size size : parcelSize.allocationOrder(properties.allocation().allowLargerSize())) {
            Optional<Compartment> candidate =
                    compartments.findFirstByStationIdAndStateAndSizeOrderByIdAsc(stationId, CompartmentState.FREE, size);
            if (candidate.isPresent()) {
                candidate.get().setState(CompartmentState.RESERVED);
                return candidate;
            }
        }
        return Optional.empty();
    }

    /** Frees a compartment after pickup / cancellation / removal (unless it is out of service). */
    public void release(Compartment compartment) {
        if (compartment != null && compartment.getState() != CompartmentState.OUT_OF_SERVICE) {
            compartment.setState(CompartmentState.FREE);
        }
    }

    // ---------- hardware ----------

    public boolean openDoor(Compartment compartment) {
        log.info("Hardware call: openDoor(compartment={})", compartment.getId());
        boolean opened;
        try {
            opened = hardware.openDoor(compartment.getId());
        } catch (RuntimeException e) {
            log.warn("Hardware error on openDoor(compartment={}): {}", compartment.getId(), e.getMessage());
            opened = false;
        }
        log.info("Hardware result: openDoor(compartment={}) -> {}", compartment.getId(), opened);
        return opened;
    }

    public boolean confirmClosed(Compartment compartment) {
        log.info("Hardware call: isDoorClosed(compartment={})", compartment.getId());
        boolean closed;
        try {
            closed = hardware.isDoorClosed(compartment.getId());
        } catch (RuntimeException e) {
            log.warn("Hardware error on isDoorClosed(compartment={}): {}", compartment.getId(), e.getMessage());
            closed = false;
        }
        log.info("Hardware result: isDoorClosed(compartment={}) -> {}", compartment.getId(), closed);
        return closed;
    }

    /** Door failure during drop-off: the compartment is put out of service and the operator is notified. */
    public void markFaulty(Compartment compartment, String reason, String trackingCode) {
        compartment.setState(CompartmentState.OUT_OF_SERVICE);
        compartment.setNeedsAttention(true);
        notifyOperator(compartment, reason, trackingCode);
    }

    /** Problem that does not change the compartment state (e.g. door failure during pickup). */
    public void flag(Compartment compartment, String reason, String trackingCode) {
        compartment.setNeedsAttention(true);
        notifyOperator(compartment, reason, trackingCode);
    }

    public void notifyOperator(Compartment compartment, String reason, String trackingCode) {
        String station = compartment == null ? "?" : String.valueOf(compartment.getStation().getId());
        String code = compartment == null ? "-" : compartment.getCode();
        notifier.send(new Notification(Notification.Type.OPERATOR_ALERT, "operator@station-" + station, trackingCode,
                "Compartment " + code + ": " + reason));
    }

    // ---------- operator ----------

    @Transactional
    public CompartmentView putOutOfService(Long compartmentId) {
        Compartment c = find(compartmentId);
        if (c.getState() != CompartmentState.FREE) {
            throw new ConflictException("COMPARTMENT_NOT_EMPTY",
                    "Only a free compartment can be put out of service (compartment " + c.getCode() + " is " + c.getState() + ")");
        }
        c.setState(CompartmentState.OUT_OF_SERVICE);
        log.info("Compartment {} put out of service by OPERATOR", c.getId());
        return CompartmentView.of(c);
    }

    @Transactional
    public CompartmentView reactivate(Long compartmentId) {
        Compartment c = find(compartmentId);
        if (c.getState() != CompartmentState.OUT_OF_SERVICE) {
            throw new ConflictException("COMPARTMENT_NOT_OUT_OF_SERVICE",
                    "Compartment " + c.getCode() + " is not out of service");
        }
        c.setState(CompartmentState.FREE);
        c.setNeedsAttention(false);
        log.info("Compartment {} reactivated by OPERATOR", c.getId());
        return CompartmentView.of(c);
    }

    @Transactional(readOnly = true)
    public List<CompartmentView> list(Long stationId) {
        return compartments.findByStationIdOrderByIdAsc(stationId).stream().map(CompartmentView::of).toList();
    }

    private Compartment find(Long compartmentId) {
        return compartments.findById(compartmentId)
                .orElseThrow(() -> new NotFoundException("COMPARTMENT_NOT_FOUND", "Compartment not found: " + compartmentId));
    }
}
