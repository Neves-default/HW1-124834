package pt.ua.tqs.tuttibox.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pt.ua.tqs.tuttibox.adapter.DisabledGeocodingClient;
import pt.ua.tqs.tuttibox.adapter.NominatimGeocodingClient;
import pt.ua.tqs.tuttibox.adapter.StubGeocodingClient;
import pt.ua.tqs.tuttibox.port.GeocodingPort;

@Configuration
public class AppConfig {

    /** All time-dependent code uses this clock, so tests can replace it with a controllable one. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** Geocoding adapter selected by configuration (tuttibox.geocoding.provider). */
    @Bean
    public GeocodingPort geocodingPort(TuttiboxProperties properties, Clock clock) {
        TuttiboxProperties.Geocoding g = properties.geocoding();
        return switch (g.provider()) {
            case "stub" -> new StubGeocodingClient();
            case "off" -> new DisabledGeocodingClient();
            default -> new NominatimGeocodingClient(g.baseUrl(), g.userAgent(), g.timeout(), g.minInterval(),
                    g.failureCacheTtl(), clock);
        };
    }
}