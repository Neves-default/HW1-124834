package pt.ua.tqs.tuttibox.port;

import pt.ua.tqs.tuttibox.domain.Coordinates;

import java.util.Optional;

/** Converts a written address into coordinates. */
public interface GeocodingPort {

    /**
     * @return the coordinates, or empty if the provider found nothing for this address
     * @throws pt.ua.tqs.tuttibox.exception.GeocodingUnavailableException if the provider is slow, down or over limits
     */
    Optional<Coordinates> geocode(String address);
}
