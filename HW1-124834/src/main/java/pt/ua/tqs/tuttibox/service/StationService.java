package pt.ua.tqs.tuttibox.service;

import org.springframework.stereotype.Service;
import pt.ua.tqs.tuttibox.config.TuttiboxProperties;
import pt.ua.tqs.tuttibox.domain.CompartmentState;
import pt.ua.tqs.tuttibox.domain.Coordinates;
import pt.ua.tqs.tuttibox.domain.GeoMath;
import pt.ua.tqs.tuttibox.domain.Size;
import pt.ua.tqs.tuttibox.exception.AddressNotFoundException;
import pt.ua.tqs.tuttibox.exception.BadRequestException;
import pt.ua.tqs.tuttibox.exception.NotFoundException;
import pt.ua.tqs.tuttibox.model.Station;
import pt.ua.tqs.tuttibox.port.GeocodingPort;
import pt.ua.tqs.tuttibox.repository.CompartmentRepository;
import pt.ua.tqs.tuttibox.repository.StationRepository;

import java.util.Comparator;
import java.util.List;

/** UC1: find the nearest stations to an address, with free compartments per size. */
@Service
public class StationService {

    private final StationRepository stations;
    private final CompartmentRepository compartments;
    private final GeocodingPort geocoding;
    private final TuttiboxProperties properties;

    public StationService(StationRepository stations, CompartmentRepository compartments,
                          GeocodingPort geocoding, TuttiboxProperties properties) {
        this.stations = stations;
        this.compartments = compartments;
        this.geocoding = geocoding;
        this.properties = properties;
    }

    /** No transaction here on purpose: the external HTTP call must not hold a database connection. */
    public List<StationView> nearest(String address) {
        if (address == null || address.isBlank()) {
            throw new BadRequestException("INVALID_REQUEST", "An address is required");
        }
        Coordinates origin = geocoding.geocode(address.trim())
                .orElseThrow(() -> new AddressNotFoundException(address.trim()));
        return stations.findAll().stream()
                .map(s -> new Ranked(s, GeoMath.distanceKm(origin, s.coordinates())))
                .sorted(Comparator.comparingDouble(Ranked::distanceKm))
                .limit(properties.search().maxResults())
                .map(r -> view(r.station(), r.distanceKm()))
                .toList();
    }

    public List<StationView> all() {
        return stations.findAll().stream()
                .sorted(Comparator.comparing(Station::getName))
                .map(s -> view(s, null))
                .toList();
    }

    public StationView get(Long stationId) {
        Station station = stations.findById(stationId)
                .orElseThrow(() -> new NotFoundException("STATION_NOT_FOUND", "Station not found: " + stationId));
        return view(station, null);
    }

    private StationView view(Station s, Double distanceKm) {
        return new StationView(s.getId(), s.getName(), s.getAddress(), distanceKm,
                free(s, Size.S), free(s, Size.M), free(s, Size.L));
    }

    private long free(Station s, Size size) {
        return compartments.countByStationIdAndStateAndSize(s.getId(), CompartmentState.FREE, size);
    }

    private record Ranked(Station station, double distanceKm) {
    }
}
