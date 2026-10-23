package pt.ua.tqs.tuttibox.exception;

/** The geocoding provider is slow, down or over its usage limits (HTTP 503). */
public class GeocodingUnavailableException extends TuttiboxException {
    public GeocodingUnavailableException(String message, Throwable cause) {
        super(503, "GEOCODING_UNAVAILABLE", message, cause);
    }

    public GeocodingUnavailableException(String message) {
        super(503, "GEOCODING_UNAVAILABLE", message);
    }
}
