package pt.ua.tqs.tuttibox.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.ua.tqs.tuttibox.model.Station;

public interface StationRepository extends JpaRepository<Station, Long> {
}
