package pt.ua.tqs.tuttibox.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import pt.ua.tqs.tuttibox.domain.CompartmentState;
import pt.ua.tqs.tuttibox.domain.Size;
import pt.ua.tqs.tuttibox.model.Compartment;

import java.util.List;
import java.util.Optional;

public interface CompartmentRepository extends JpaRepository<Compartment, Long> {

    /**
     * Picks one free compartment of the given size and locks its row (SELECT ... FOR UPDATE).
     * Lock timeout -2 = SKIP LOCKED: a concurrent request skips a row that another transaction
     * is reserving, so two partners never get the same compartment.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    Optional<Compartment> findFirstByStationIdAndStateAndSizeOrderByIdAsc(Long stationId, CompartmentState state, Size size);

    long countByStationIdAndStateAndSize(Long stationId, CompartmentState state, Size size);

    List<Compartment> findByStationIdOrderByIdAsc(Long stationId);
}
