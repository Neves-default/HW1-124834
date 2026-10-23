package pt.ua.tqs.tuttibox.testdoubles;

import java.time.Duration;

import pt.ua.tqs.tuttibox.config.TuttiboxProperties;

/** The same values as application.yml, for unit tests without Spring. */
public final class TestProperties {

    private TestProperties() {
    }

    public static TuttiboxProperties defaults() {
        return new TuttiboxProperties(
                new TuttiboxProperties.Deadlines(Duration.ofDays(5), Duration.ofDays(5)),
                new TuttiboxProperties.PickupCode(6, 5, Duration.ofMinutes(2), "test-secret"),
                new TuttiboxProperties.Allocation(true),
                new TuttiboxProperties.Search(5),
                new TuttiboxProperties.Geocoding("off", "http://localhost", Duration.ofSeconds(5),
                        Duration.ofSeconds(1), Duration.ofSeconds(60), "TuttiBox-test"));
    }
}
