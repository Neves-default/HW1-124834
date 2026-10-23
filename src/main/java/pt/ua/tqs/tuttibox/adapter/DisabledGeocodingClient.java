package pt.ua.tqs.tuttibox.adapter;

import pt.ua.tqs.tuttibox.domain.Coordinates;
import pt.ua.tqs.tuttibox.exception.GeocodingUnavailableException;
import pt.ua.tqs.tuttibox.port.GeocodingPort;

import java.util.Optional;

/** Geocoding switched off by configuration (tuttibox.geocoding.provider=off), as the Nominatim policy requires. */
public class DisabledGeocodingClient implements GeocodingPort {

    @Override
    public Optional<Coordinates> geocode(String address) {
        throw new GeocodingUnavailableException("Geocoding is disabled by configuration");
    }
}
