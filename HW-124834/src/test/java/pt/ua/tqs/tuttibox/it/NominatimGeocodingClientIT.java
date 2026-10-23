package pt.ua.tqs.tuttibox.it;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

import pt.ua.tqs.tuttibox.adapter.NominatimGeocodingClient;
import pt.ua.tqs.tuttibox.domain.Coordinates;
import pt.ua.tqs.tuttibox.exception.GeocodingUnavailableException;
import pt.ua.tqs.tuttibox.testdoubles.MutableClock;

/**
 * The Nominatim client against a stubbed HTTP server (WireMock): found, not found, slow, down,
 * over limits, cache, User-Agent and rate limit, and that the same address is never sent twice in a
 * short interval (failures remembered, concurrent searches merged). The real API is never called.
 */
class NominatimGeocodingClientIT {

    private static final Duration TIMEOUT = Duration.ofMillis(500);
    private static final Duration MIN_INTERVAL = Duration.ofMillis(300);
    private static final Duration FAILURE_CACHE_TTL = Duration.ofSeconds(60);

    private WireMockServer server;
    private MutableClock clock;
    private NominatimGeocodingClient client;

    @BeforeEach
    void start() {
        server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();
        clock = new MutableClock();
        client = new NominatimGeocodingClient(server.baseUrl(), "TuttiBox-test/1.0 (test@ua.pt)", TIMEOUT, MIN_INTERVAL,
                FAILURE_CACHE_TTL, clock);
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    private void respond(String body) {
        server.stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)));
    }

    @Test
    void returnsCoordinatesOfTheFirstResult() {
        respond("""
                [{"place_id": 1, "lat": "40.6405", "lon": "-8.6538", "display_name": "Aveiro, Portugal"}]
                """);

        assertThat(client.geocode("Aveiro")).contains(new Coordinates(40.6405, -8.6538));
        server.verify(getRequestedFor(urlPathEqualTo("/search"))
                .withQueryParam("q", equalTo("Aveiro"))
                .withQueryParam("format", equalTo("jsonv2")));
    }

    @Test
    void emptyAnswerMeansAddressNotFound() {
        respond("[]");

        assertThat(client.geocode("Rua que não existe")).isEmpty();
    }

    @Test
    void serverErrorMeansUnavailable() {
        server.stubFor(get(urlPathEqualTo("/search")).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> client.geocode("Aveiro")).isInstanceOf(GeocodingUnavailableException.class);
    }

    @Test
    void tooManyRequestsMeansUnavailable() {
        server.stubFor(get(urlPathEqualTo("/search")).willReturn(aResponse().withStatus(429)));

        assertThatThrownBy(() -> client.geocode("Aveiro")).isInstanceOf(GeocodingUnavailableException.class);
    }

    @Test
    void slowAnswerTimesOut() {
        server.stubFor(get(urlPathEqualTo("/search")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json").withBody("[]")
                .withFixedDelay((int) TIMEOUT.toMillis() * 3)));

        long start = System.nanoTime();
        assertThatThrownBy(() -> client.geocode("Aveiro")).isInstanceOf(GeocodingUnavailableException.class);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(TIMEOUT.multipliedBy(3));
    }

    @Test
    void sameAddressIsRequestedOnlyOnce() {
        respond("""
                [{"lat": "40.6405", "lon": "-8.6538", "display_name": "Aveiro"}]
                """);

        client.geocode("Rua de Aveiro 10");
        client.geocode("  rua de aveiro   10 ");

        server.verify(1, getRequestedFor(urlPathEqualTo("/search")));
    }

    @Test
    void everyRequestSendsAnIdentifyingUserAgent() {
        respond("[]");

        client.geocode("Aveiro");

        server.verify(getRequestedFor(urlPathEqualTo("/search")).withHeader("User-Agent", containing("TuttiBox")));
    }

    @Test
    void requestsAreSpacedByTheMinimumInterval() {
        respond("[]");

        client.geocode("address one");
        client.geocode("address two");

        List<ServeEvent> events = server.getAllServeEvents().stream()
                .sorted(Comparator.comparing(e -> e.getRequest().getLoggedDate()))
                .toList();
        assertThat(events).hasSize(2);
        long gapMs = events.get(1).getRequest().getLoggedDate().getTime() - events.get(0).getRequest().getLoggedDate().getTime();
        assertThat(gapMs).isGreaterThanOrEqualTo(MIN_INTERVAL.toMillis() - 20);
    }

    @Test
    void failedAddressIsNotRequestedAgainWithinTheFailureWindow() {
        server.stubFor(get(urlPathEqualTo("/search")).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> client.geocode("Aveiro")).isInstanceOf(GeocodingUnavailableException.class);
        clock.advance(FAILURE_CACHE_TTL.minusSeconds(1));
        assertThatThrownBy(() -> client.geocode("  aveiro ")).isInstanceOf(GeocodingUnavailableException.class);

        server.verify(1, getRequestedFor(urlPathEqualTo("/search")));
    }

    @Test
    void failedAddressIsRetriedAfterTheFailureWindow() {
        server.stubFor(get(urlPathEqualTo("/search")).willReturn(aResponse().withStatus(503)));
        assertThatThrownBy(() -> client.geocode("Aveiro")).isInstanceOf(GeocodingUnavailableException.class);

        respond("""
                [{"lat": "40.6405", "lon": "-8.6538", "display_name": "Aveiro"}]
                """);
        clock.advance(FAILURE_CACHE_TTL);

        assertThat(client.geocode("Aveiro")).contains(new Coordinates(40.6405, -8.6538));
        server.verify(2, getRequestedFor(urlPathEqualTo("/search")));
    }

    @Test
    void concurrentSearchesForTheSameAddressSendOneRequest() throws Exception {
        server.stubFor(get(urlPathEqualTo("/search")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("[{\"lat\": \"40.6405\", \"lon\": \"-8.6538\", \"display_name\": \"Aveiro\"}]")
                .withFixedDelay(200)));
        int users = 5;
        ExecutorService pool = Executors.newFixedThreadPool(users);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> searches = new ArrayList<>();
            for (int i = 0; i < users; i++) {
                searches.add(pool.submit(() -> {
                    start.await();
                    return client.geocode("Rua de Aveiro 10");
                }));
            }
            start.countDown();
            for (Future<?> search : searches) {
                assertThat(search.get(10, TimeUnit.SECONDS)).isEqualTo(Optional.of(new Coordinates(40.6405, -8.6538)));
            }
        } finally {
            pool.shutdownNow();
        }

        server.verify(1, getRequestedFor(urlPathEqualTo("/search")));
    }
}
