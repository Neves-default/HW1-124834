package pt.ua.tqs.tuttibox.testdoubles;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.ua.tqs.tuttibox.domain.CompartmentState;
import pt.ua.tqs.tuttibox.domain.ParcelState;
import pt.ua.tqs.tuttibox.domain.Size;
import pt.ua.tqs.tuttibox.model.Compartment;
import pt.ua.tqs.tuttibox.model.Parcel;
import pt.ua.tqs.tuttibox.model.Station;
import pt.ua.tqs.tuttibox.repository.CompartmentRepository;
import pt.ua.tqs.tuttibox.repository.ParcelRepository;
import pt.ua.tqs.tuttibox.repository.StationRepository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Builds and inspects test data in its own transactions (tests themselves are not transactional). */
public class TestData {

    private final StationRepository stations;
    private final CompartmentRepository compartments;
    private final ParcelRepository parcels;
    private final TransactionTemplate tx;

    public TestData(StationRepository stations, CompartmentRepository compartments, ParcelRepository parcels,
                    PlatformTransactionManager transactionManager) {
        this.stations = stations;
        this.compartments = compartments;
        this.parcels = parcels;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public void reset() {
        tx.executeWithoutResult(s -> {
            parcels.deleteAll();
            compartments.deleteAll();
            stations.deleteAll();
        });
    }

    /** A station with one compartment per given size, named S1, M1, M2... */
    public Long station(String name, double latitude, double longitude, Size... sizes) {
        return tx.execute(s -> {
            Station station = stations.save(new Station(name, name + " address", latitude, longitude));
            Map<Size, Integer> counters = new EnumMap<>(Size.class);
            for (Size size : sizes) {
                int n = counters.merge(size, 1, Integer::sum);
                compartments.save(new Compartment(station, size.name() + n, size));
            }
            return station.getId();
        });
    }

    public List<Long> compartmentIds(Long stationId) {
        return tx.execute(s -> compartments.findByStationIdOrderByIdAsc(stationId).stream().map(Compartment::getId).toList());
    }

    public Long compartmentId(Long stationId, String code) {
        return tx.execute(s -> compartments.findByStationIdOrderByIdAsc(stationId).stream()
                .filter(c -> c.getCode().equals(code)).findFirst().orElseThrow().getId());
    }

    public CompartmentState compartmentState(Long compartmentId) {
        return tx.execute(s -> compartments.findById(compartmentId).orElseThrow().getState());
    }

    public boolean compartmentNeedsAttention(Long compartmentId) {
        return Boolean.TRUE.equals(tx.execute(s -> compartments.findById(compartmentId).orElseThrow().isNeedsAttention()));
    }

    public Size compartmentSize(Long compartmentId) {
        return tx.execute(s -> compartments.findById(compartmentId).orElseThrow().getSize());
    }

    public ParcelState parcelState(String trackingCode) {
        return tx.execute(s -> parcel(trackingCode).getState());
    }

    /** null if the parcel has no compartment */
    public Long parcelCompartmentId(String trackingCode) {
        return tx.execute(s -> {
            Compartment c = parcel(trackingCode).getCompartment();
            return c == null ? null : c.getId();
        });
    }

    public String parcelPickupCodeHash(String trackingCode) {
        return tx.execute(s -> parcel(trackingCode).getPickupCodeHash());
    }

    public long parcelCount() {
        return parcels.count();
    }

    private Parcel parcel(String trackingCode) {
        return parcels.findByTrackingCode(trackingCode).orElseThrow();
    }
}
