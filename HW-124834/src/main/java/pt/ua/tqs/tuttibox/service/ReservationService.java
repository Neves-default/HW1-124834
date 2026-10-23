package pt.ua.tqs.tuttibox.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.ua.tqs.tuttibox.config.TuttiboxProperties;
import pt.ua.tqs.tuttibox.domain.Actor;
import pt.ua.tqs.tuttibox.domain.CompartmentState;
import pt.ua.tqs.tuttibox.domain.ParcelEvent;
import pt.ua.tqs.tuttibox.domain.ParcelLifecycle;
import pt.ua.tqs.tuttibox.domain.ParcelState;
import pt.ua.tqs.tuttibox.domain.Size;
import pt.ua.tqs.tuttibox.domain.TerminalLockout;
import pt.ua.tqs.tuttibox.exception.BadRequestException;
import pt.ua.tqs.tuttibox.exception.ConflictException;
import pt.ua.tqs.tuttibox.exception.NotFoundException;
import pt.ua.tqs.tuttibox.exception.TerminalBlockedException;
import pt.ua.tqs.tuttibox.model.Compartment;
import pt.ua.tqs.tuttibox.model.Parcel;
import pt.ua.tqs.tuttibox.model.Station;
import pt.ua.tqs.tuttibox.port.Notification;
import pt.ua.tqs.tuttibox.port.NotificationPort;
import pt.ua.tqs.tuttibox.repository.ParcelRepository;
import pt.ua.tqs.tuttibox.repository.StationRepository;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** The parcel lifecycle: registration, cancellation, drop-off, pickup, deadlines, operator removal, tracking. */
@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    private static final String TRACKING_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int MAX_DOOR_RETRIES = 5;

    private final SecureRandom random = new SecureRandom();
    private final StationRepository stations;
    private final ParcelRepository parcels;
    private final CompartmentService compartments;
    private final PickupCodeService pickupCodes;
    private final NotificationPort notifier;
    private final TuttiboxProperties properties;
    private final Clock clock;

    public ReservationService(StationRepository stations, ParcelRepository parcels, CompartmentService compartments,
                              PickupCodeService pickupCodes, NotificationPort notifier,
                              TuttiboxProperties properties, Clock clock) {
        this.stations = stations;
        this.parcels = parcels;
        this.compartments = compartments;
        this.pickupCodes = pickupCodes;
        this.notifier = notifier;
        this.properties = properties;
        this.clock = clock;
    }

    // ===================== UC2: partner =====================

    @Transactional
    public RegistrationResult register(Long stationId, Size size, String recipientContact) {
        if (stationId == null || size == null || recipientContact == null || recipientContact.isBlank()) {
            throw new BadRequestException("INVALID_REQUEST", "stationId, size and recipientContact are required");
        }
        Station station = stations.findById(stationId)
                .orElseThrow(() -> new BadRequestException("UNKNOWN_STATION", "Unknown station: " + stationId));
        Compartment compartment = compartments.allocate(stationId, size)
                .orElseThrow(() -> new ConflictException("NO_FREE_COMPARTMENT",
                        "Station '" + station.getName() + "' has no free compartment for size " + size));

        Parcel parcel = Parcel.register(newTrackingCode(), station, compartment, size, recipientContact.trim(), now());
        parcels.save(parcel);
        log.info("Parcel {}: (new) -> CREATED (actor PARTNER), station {}, compartment {}",
                parcel.getTrackingCode(), stationId, compartment.getCode());
        return new RegistrationResult(parcel.getTrackingCode(), parcel.getState(), stationId,
                compartment.getCode(), compartment.getSize());
    }

    @Transactional
    public TrackingView cancel(String trackingCode) {
        Parcel parcel = find(trackingCode);
        change(parcel, ParcelEvent.CANCEL, Actor.PARTNER);
        compartments.release(parcel.getCompartment());
        return TrackingView.of(parcel);
    }

    // ===================== UC3: courier at the terminal =====================

    @Transactional
    public TerminalResult dropOff(Long stationId, String trackingCode) {
        Parcel parcel = findAtStation(stationId, trackingCode);
        ParcelLifecycle.next(parcel.getState(), ParcelEvent.DEPOSIT); // reject before touching the hardware

        Compartment compartment = openCompartmentForDropOff(parcel);
        if (compartment == null) {
            compartments.notifyOperator(null, "parcel " + trackingCode + " could not be deposited: no working compartment",
                    trackingCode);
            return TerminalResult.hardwareFailure(
                    "The compartment could not be opened and no other compartment is free. The operator was notified.");
        }
        if (!compartments.confirmClosed(compartment)) {
            compartments.flag(compartment, "door not confirmed closed after drop-off", trackingCode);
            return TerminalResult.hardwareFailure(
                    "The door was not confirmed closed, so the parcel was not registered as deposited. The operator was notified.");
        }

        compartment.setState(CompartmentState.OCCUPIED);
        String code = newUniquePickupCode(stationId);
        change(parcel, ParcelEvent.DEPOSIT, Actor.COURIER);
        parcel.setPickupCodeHash(pickupCodes.hash(stationId, code));
        notifier.send(new Notification(Notification.Type.PICKUP_CODE, parcel.getRecipientContact(), trackingCode,
                "Your parcel " + trackingCode + " is ready at " + parcel.getStation().getName()
                        + ". Pickup code: " + code));
        return TerminalResult.ok("Parcel deposited in compartment " + compartment.getCode() + ".", compartment.getCode());
    }

    /**
     * Opens the reserved compartment. If its door fails, the compartment goes out of service and
     * the parcel is moved to another free compatible compartment. Returns null if none works.
     */
    private Compartment openCompartmentForDropOff(Parcel parcel) {
        Compartment compartment = parcel.getCompartment();
        for (int attempt = 0; attempt < MAX_DOOR_RETRIES; attempt++) {
            if (compartment == null) {
                compartment = compartments.allocate(parcel.getStation().getId(), parcel.getSize()).orElse(null);
                if (compartment == null) {
                    parcel.setCompartment(null);
                    return null;
                }
                parcel.setCompartment(compartment);
                log.info("Parcel {} moved to compartment {}", parcel.getTrackingCode(), compartment.getCode());
            }
            if (compartments.openDoor(compartment)) {
                return compartment;
            }
            compartments.markFaulty(compartment, "door did not open during drop-off", parcel.getTrackingCode());
            compartment = null;
        }
        parcel.setCompartment(null);
        return null;
    }

    // ===================== UC4: recipient at the terminal =====================

    @Transactional
    public TerminalResult pickup(Long stationId, String pickupCode) {
        Station station = stations.findById(stationId)
                .orElseThrow(() -> new NotFoundException("STATION_NOT_FOUND", "Station not found: " + stationId));
        Instant now = now();
        TerminalLockout lockout = station.getLockout();
        if (lockout.isBlocked(now)) {
            throw new TerminalBlockedException(lockout.getBlockedUntil());
        }

        Optional<Parcel> match = pickupCodes.isWellFormed(pickupCode)
                ? parcels.findByStationIdAndPickupCodeHashAndState(stationId,
                        pickupCodes.hash(stationId, pickupCode), ParcelState.READY_FOR_PICKUP)
                : Optional.empty();
        if (match.isEmpty()) {
            lockout.registerFailure(now, properties.pickupCode().maxAttempts(), properties.pickupCode().lockout());
            log.info("Wrong pickup code at station {} (blocked: {})", stationId, lockout.isBlocked(now));
            return TerminalResult.invalidCode("Invalid pickup code.");
        }
        lockout.reset();

        Parcel parcel = match.get();
        Compartment compartment = parcel.getCompartment();
        if (!compartments.openDoor(compartment)) {
            compartments.flag(compartment, "door did not open during pickup", parcel.getTrackingCode());
            return TerminalResult.hardwareFailure(
                    "The compartment could not be opened. Your code is still valid and the operator was notified.");
        }
        change(parcel, ParcelEvent.PICKUP, Actor.RECIPIENT);
        compartments.release(compartment);
        return TerminalResult.ok("Compartment " + compartment.getCode() + " is open. Please collect your parcel.",
                compartment.getCode());
    }

    // ===================== deadlines (system) =====================

    /** Cancels parcels not deposited in time and expires parcels not collected in time. */
    @Transactional
    public int applyDeadlines() {
        Instant now = now();
        int changed = 0;
        for (Parcel p : parcels.findByStateAndCreatedAtBefore(ParcelState.CREATED,
                now.minus(properties.deadlines().dropOff()))) {
            change(p, ParcelEvent.DROP_OFF_DEADLINE, Actor.SYSTEM);
            compartments.release(p.getCompartment());
            changed++;
        }
        for (Parcel p : parcels.findByStateAndReadyAtBefore(ParcelState.READY_FOR_PICKUP,
                now.minus(properties.deadlines().pickup()))) {
            change(p, ParcelEvent.PICKUP_DEADLINE, Actor.SYSTEM);
            changed++;
        }
        return changed;
    }

    // ===================== UC6: operator =====================

    @Transactional
    public TrackingView registerRemoval(String trackingCode) {
        Parcel parcel = find(trackingCode);
        change(parcel, ParcelEvent.REMOVE, Actor.OPERATOR);
        compartments.release(parcel.getCompartment());
        return TrackingView.of(parcel);
    }

    @Transactional(readOnly = true)
    public List<ParcelSummary> listParcels(Long stationId, ParcelState state) {
        List<Parcel> list = state == null
                ? parcels.findByStationIdOrderByCreatedAtAsc(stationId)
                : parcels.findByStationIdAndStateOrderByCreatedAtAsc(stationId, state);
        return list.stream().map(ParcelSummary::of).toList();
    }

    // ===================== UC5: tracking =====================

    @Transactional(readOnly = true)
    public TrackingView track(String trackingCode) {
        return TrackingView.of(find(trackingCode));
    }

    // ===================== helpers =====================

    private void change(Parcel parcel, ParcelEvent event, Actor actor) {
        ParcelState from = parcel.getState();
        ParcelState to = parcel.apply(event, actor, now());
        log.info("Parcel {}: {} -> {} (event {}, actor {})", parcel.getTrackingCode(), from, to, event, actor);
    }

    private Parcel find(String trackingCode) {
        return parcels.findByTrackingCode(trackingCode == null ? "" : trackingCode.trim())
                .orElseThrow(() -> new NotFoundException("PARCEL_NOT_FOUND", "Parcel not found: " + trackingCode));
    }

    private Parcel findAtStation(Long stationId, String trackingCode) {
        Parcel parcel = find(trackingCode);
        if (!parcel.getStation().getId().equals(stationId)) {
            throw new NotFoundException("PARCEL_NOT_FOUND", "Parcel " + trackingCode + " is not for this station");
        }
        return parcel;
    }

    private String newTrackingCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("TBX-");
            for (int i = 0; i < 8; i++) {
                sb.append(TRACKING_ALPHABET.charAt(random.nextInt(TRACKING_ALPHABET.length())));
            }
            code = sb.toString();
        } while (parcels.existsByTrackingCode(code));
        return code;
    }

    /** A code that no other parcel waiting at the same station is using. */
    private String newUniquePickupCode(Long stationId) {
        for (int i = 0; i < 20; i++) {
            String code = pickupCodes.newCode();
            if (!parcels.existsByStationIdAndPickupCodeHashAndState(stationId, pickupCodes.hash(stationId, code),
                    ParcelState.READY_FOR_PICKUP)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique pickup code");
    }

    private Instant now() {
        return clock.instant();
    }
}
