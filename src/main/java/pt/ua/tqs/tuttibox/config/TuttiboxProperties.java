package pt.ua.tqs.tuttibox.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** All business-rule values, configurable in application.yml under the prefix "tuttibox". */
@ConfigurationProperties("tuttibox")
public record TuttiboxProperties(
        Deadlines deadlines,
        PickupCode pickupCode,
        Allocation allocation,
        Search search,
        Geocoding geocoding) {

    /** Drop-off window after registration, pickup window after drop-off. */
    public record Deadlines(Duration dropOff, Duration pickup) {
    }

    /** Code length, consecutive wrong attempts before lockout, lockout duration, HMAC secret for storage. */
    public record PickupCode(int length, int maxAttempts, Duration lockout, String secret) {
    }

    /** If true, a parcel may go to a larger compartment when its exact size is full. */
    public record Allocation(boolean allowLargerSize) {
    }

    /** Number of nearest stations shown. */
    public record Search(int maxResults) {
    }

    /**
     * provider: nominatim | stub | off. failureCacheTtl: how long a failed lookup is remembered,
     * so the same address is not sent to the provider again within that window.
     */
    public record Geocoding(String provider, String baseUrl, Duration timeout, Duration minInterval,
                            Duration failureCacheTtl, String userAgent) {
    }
}