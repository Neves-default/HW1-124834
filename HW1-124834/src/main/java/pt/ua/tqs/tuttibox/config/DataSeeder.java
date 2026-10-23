package pt.ua.tqs.tuttibox.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.ua.tqs.tuttibox.domain.Size;
import pt.ua.tqs.tuttibox.model.Compartment;
import pt.ua.tqs.tuttibox.model.Station;
import pt.ua.tqs.tuttibox.repository.CompartmentRepository;
import pt.ua.tqs.tuttibox.repository.StationRepository;

import java.util.List;

/** Staging data: stations with real coordinates in Aveiro, each with S/M/L compartments. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final StationRepository stations;
    private final CompartmentRepository compartments;
    private final boolean enabled;

    public DataSeeder(StationRepository stations, CompartmentRepository compartments,
                      @Value("${tuttibox.seed-demo-data:true}") boolean enabled) {
        this.stations = stations;
        this.compartments = compartments;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || stations.count() > 0) {
            return;
        }
        List<Station> seed = List.of(
                new Station("Aveiro Railway Station", "Largo da Estação, Aveiro", 40.6439, -8.6403),
                new Station("University of Aveiro - Santiago Campus", "Campus Universitário de Santiago, Aveiro", 40.6303, -8.6575),
                new Station("Fórum Aveiro", "Rua Batalhão Caçadores 10, Aveiro", 40.6413, -8.6527),
                new Station("Glicínias Plaza", "Rua D. Manuel Barbosa 1, Aveiro", 40.6290, -8.6420),
                new Station("Mercado Manuel Firmino", "Praça Melo Freitas, Aveiro", 40.6395, -8.6497),
                new Station("Aveiro Retail Park", "Rua Senhora da Alumieira, Aveiro", 40.6540, -8.6280));
        for (Station station : seed) {
            stations.save(station);
            addCompartments(station, Size.S, 3);
            addCompartments(station, Size.M, 3);
            addCompartments(station, Size.L, 2);
        }
        log.info("Seeded {} stations", seed.size());
    }

    private void addCompartments(Station station, Size size, int count) {
        for (int i = 1; i <= count; i++) {
            compartments.save(new Compartment(station, size.name() + i, size));
        }
    }
}
