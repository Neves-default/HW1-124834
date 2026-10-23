package pt.ua.tqs.tuttibox.testdoubles;

import pt.ua.tqs.tuttibox.domain.Coordinates;
import pt.ua.tqs.tuttibox.exception.GeocodingUnavailableException;
import pt.ua.tqs.tuttibox.port.GeocodingPort;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Fake geocoder: an in-memory address book. Can simulate an unavailable provider. */
public class FakeGeocoder implements GeocodingPort {

    private final Map<String, Coordinates> addresses = new ConcurrentHashMap<>();
    private volatile boolean unavailable;

    public FakeGeocoder knows(String address, double latitude, double longitude) {
        addresses.put(key(address), new Coordinates(latitude, longitude));
        return this;
    }

    public void setUnavailable(boolean unavailable) {
        this.unavailable = unavailable;
    }

    public void reset() {
        addresses.clear();
        unavailable = false;
    }

    @Override
    public Optional<Coordinates> geocode(String address) {
        if (unavailable) {
            throw new GeocodingUnavailableException("Fake provider is down");
        }
        return Optional.ofNullable(addresses.get(key(address)));
    }

    private static String key(String address) {
        return address.trim().toLowerCase(Locale.ROOT);
    }
}
