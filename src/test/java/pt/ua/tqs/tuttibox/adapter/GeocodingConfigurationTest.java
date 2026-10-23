package pt.ua.tqs.tuttibox.adapter;

import org.junit.jupiter.api.Test;
import pt.ua.tqs.tuttibox.config.AppConfig;
import pt.ua.tqs.tuttibox.config.TuttiboxProperties;
import pt.ua.tqs.tuttibox.exception.GeocodingUnavailableException;
import pt.ua.tqs.tuttibox.port.GeocodingPort;
import pt.ua.tqs.tuttibox.testdoubles.TestProperties;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** NFR4: the geocoding provider can be switched (or turned off) by configuration, without code changes. */
class GeocodingConfigurationTest {

    private static TuttiboxProperties withProvider(String provider) {
        TuttiboxProperties d = TestProperties.defaults();
        return new TuttiboxProperties(d.deadlines(), d.pickupCode(), d.allocation(), d.search(),
                new TuttiboxProperties.Geocoding(provider, "http://localhost:1", Duration.ofSeconds(1),
                        Duration.ofSeconds(1), Duration.ofSeconds(60), "TuttiBox-test"));
    }

    @Test
    void providerIsChosenByConfiguration() {
        AppConfig config = new AppConfig();
        Clock clock = config.clock();

        assertThat(config.geocodingPort(withProvider("nominatim"), clock)).isInstanceOf(NominatimGeocodingClient.class);
        assertThat(config.geocodingPort(withProvider("stub"), clock)).isInstanceOf(StubGeocodingClient.class);
        assertThat(config.geocodingPort(withProvider("off"), clock)).isInstanceOf(DisabledGeocodingClient.class);
        assertThat(config.clock()).isNotNull();
    }

    @Test
    void offMeansUnavailable() {
        GeocodingPort off = new DisabledGeocodingClient();

        assertThatThrownBy(() -> off.geocode("Aveiro")).isInstanceOf(GeocodingUnavailableException.class);
    }

    @Test
    void stubAlwaysAnswersWithoutCallingTheProvider() {
        assertThat(new StubGeocodingClient().geocode("anything")).contains(StubGeocodingClient.AVEIRO_CENTRE);
    }
}
