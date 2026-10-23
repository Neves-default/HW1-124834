package pt.ua.tqs.tuttibox.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.ua.tqs.tuttibox.domain.ParcelState;
import pt.ua.tqs.tuttibox.model.Parcel;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ParcelRepository extends JpaRepository<Parcel, Long> {

    Optional<Parcel> findByTrackingCode(String trackingCode);

    boolean existsByTrackingCode(String trackingCode);

    Optional<Parcel> findByStationIdAndPickupCodeHashAndState(Long stationId, String pickupCodeHash, ParcelState state);

    boolean existsByStationIdAndPickupCodeHashAndState(Long stationId, String pickupCodeHash, ParcelState state);

    List<Parcel> findByStationIdOrderByCreatedAtAsc(Long stationId);

    List<Parcel> findByStationIdAndStateOrderByCreatedAtAsc(Long stationId, ParcelState state);

    List<Parcel> findByStateAndCreatedAtBefore(ParcelState state, Instant limit);

    List<Parcel> findByStateAndReadyAtBefore(ParcelState state, Instant limit);
}
