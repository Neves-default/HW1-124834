package pt.ua.tqs.tuttibox.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.ua.tqs.tuttibox.domain.Coordinates;
import pt.ua.tqs.tuttibox.exception.AddressNotFoundException;
import pt.ua.tqs.tuttibox.exception.BadRequestException;
import pt.ua.tqs.tuttibox.model.Station;
import pt.ua.tqs.tuttibox.port.GeocodingPort;
import pt.ua.tqs.tuttibox.repository.CompartmentRepository;
import pt.ua.tqs.tuttibox.repository.StationRepository;
import pt.ua.tqs.tuttibox.testdoubles.TestProperties;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Ranking of stations by distance (unit level). Mocks are enough here: we only need fixed answers
 * from the repositories and the geocoder, no state.
 */
class StationServiceTest {

    private static final String ADDRESS = "Rua de Aveiro 10";

    private final StationRepository stations = mock(StationRepository.class);
    private final CompartmentRepository compartments = mock(CompartmentRepository.class);
    private final GeocodingPort geocoding = mock(GeocodingPort.class);
    private StationService service;

    @BeforeEach
    void setUp() {
        service = new StationService(stations, compartments, geocoding, TestProperties.defaults());
        when(compartments.countByStationIdAndStateAndSize(any(), any(), any())).thenReturn(1L);
        when(geocoding.geocode(ADDRESS)).thenReturn(Optional.of(new Coordinates(40.6405, -8.6538)));
    }

    @Test
    void returnsTheFiveNearestStationsOrderedByDistance() {
        when(stations.findAll()).thenReturn(List.of(
                new Station("far", "a", 40.70, -8.60),
                new Station("nearest", "a", 40.6406, -8.6539),
                new Station("third", "a", 40.645, -8.66),
                new Station("second", "a", 40.642, -8.655),
                new Station("fourth", "a", 40.65, -8.67),
                new Station("fifth", "a", 40.66, -8.68)));

        List<StationView> result = service.nearest(ADDRESS);

        assertThat(result).extracting(StationView::name)
                .containsExactly("nearest", "second", "third", "fourth", "fifth");
        assertThat(result).extracting(StationView::distanceKm).isSorted();
        assertThat(result.get(0).freeM()).isEqualTo(1);
    }

    @Test
    void unknownAddressIsReported() {
        when(geocoding.geocode("nowhere")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.nearest("nowhere")).isInstanceOf(AddressNotFoundException.class);
    }

    @Test
    void blankAddressIsRejected() {
        assertThatThrownBy(() -> service.nearest("  ")).isInstanceOf(BadRequestException.class);
    }
}
