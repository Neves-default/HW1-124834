package pt.ua.tqs.tuttibox.adapter;

import pt.ua.tqs.tuttibox.domain.Coordinates;
import pt.ua.tqs.tuttibox.port.GeocodingPort;

import java.util.Optional;

/**
 * Offline geocoder used for load tests and offline demos (tuttibox.geocoding.provider=stub):
 * every address resolves to the centre of Aveiro, so the real provider is never called.
 */
public class StubGeocodingClient implements GeocodingPort {

    static final Coordinates AVEIRO_CENTRE = new Coordinates(40.6405, -8.6538);

    @Override
    public Optional<Coordinates> geocode(String address) {
        return Optional.of(AVEIRO_CENTRE);
    }
}
