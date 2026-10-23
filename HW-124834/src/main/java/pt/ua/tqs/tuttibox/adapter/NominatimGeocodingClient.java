package pt.ua.tqs.tuttibox.adapter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import pt.ua.tqs.tuttibox.domain.Coordinates;
import pt.ua.tqs.tuttibox.exception.GeocodingUnavailableException;
import pt.ua.tqs.tuttibox.port.GeocodingPort;

/**
 * Geocoding adapter for OSM Nominatim. Follows its usage policy
 * (https://operations.osmfoundation.org/policies/nominatim/):
 * identifying User-Agent, at most 1 request per second, results cached, no autocomplete.
 * Slow (timeout), failing (5xx) or over-limit (429) answers become GeocodingUnavailableException.
 * <p>
 * The same address is never sent twice in a short interval:
 * <ul>
 *   <li>answers (found / not found) are cached;</li>
 *   <li>failures are remembered for {@code failureCacheTtl}, so a failing address is not retried at once
 *       (it is not cached forever, otherwise a short outage would make the address unsearchable);</li>
 *   <li>concurrent searches for the same address wait for the first request instead of sending their own.</li>
 * </ul>
 */
public class NominatimGeocodingClient implements GeocodingPort {

    private static final Logger log = LoggerFactory.getLogger(NominatimGeocodingClient.class);

    private final RestClient restClient;
    private final Duration minInterval;
    private final Duration failureCacheTtl;
    private final Clock clock;
    private final Map<String, Optional<Coordinates>> cache = new ConcurrentHashMap<>();
    private final Map<String, Instant> failedUntil = new ConcurrentHashMap<>();
    private final Object throttleLock = new Object();
    private long lastCallNanos;

    public NominatimGeocodingClient(String baseUrl, String userAgent, Duration timeout, Duration minInterval,
                                    Duration failureCacheTtl, Clock clock) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .build();
        this.minInterval = minInterval;
        this.failureCacheTtl = failureCacheTtl;
        this.clock = clock;
    }

    @Override
    public Optional<Coordinates> geocode(String address) {
        String key = normalize(address);
        // Entries are never removed, so containsKey followed by get is safe without a lock.
        if (cache.containsKey(key)) {
            log.info("Geocoding cache hit for '{}'", key);
            return cache.get(key);
        }
        rejectIfRecentlyFailed(key);
        synchronized (throttleLock) {
            // Another search for the same address may have finished while this one was waiting.
            if (cache.containsKey(key)) {
                log.info("Geocoding cache hit for '{}' (resolved by a concurrent search)", key);
                return cache.get(key);
            }
            rejectIfRecentlyFailed(key);
            waitForRateLimit();
            try {
                Optional<Coordinates> result = callProvider(address);
                cache.put(key, result);
                return result;
            } catch (GeocodingUnavailableException e) {
                failedUntil.put(key, clock.instant().plus(failureCacheTtl));
                throw e;
            }
        }
    }

    /** A recent failure for this address is answered locally, without calling the provider again. */
    private void rejectIfRecentlyFailed(String key) {
        Instant until = failedUntil.get(key);
        if (until == null) {
            return;
        }
        if (clock.instant().isBefore(until)) {
            log.info("Geocoding for '{}' failed recently; not calling the provider again before {}", key, until);
            throw new GeocodingUnavailableException("The geocoding service is temporarily unavailable");
        }
        failedUntil.remove(key, until);
    }

    /** Must be called while holding throttleLock. */
    private Optional<Coordinates> callProvider(String address) {
        log.info("Geocoding request to Nominatim for '{}'", address);
        try {
            NominatimPlace[] places = restClient.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("q", address)
                            .queryParam("format", "jsonv2")
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .body(NominatimPlace[].class);
            if (places == null || places.length == 0) {
                log.info("Geocoding: no result for '{}'", address);
                return Optional.empty();
            }
            Coordinates coordinates = new Coordinates(
                    Double.parseDouble(places[0].lat()), Double.parseDouble(places[0].lon()));
            log.info("Geocoding: '{}' -> {}", address, coordinates);
            return Optional.of(coordinates);
        } catch (RestClientException | NumberFormatException e) {
            log.warn("Geocoding provider unavailable for '{}': {}", address, e.getMessage());
            throw new GeocodingUnavailableException("The geocoding service is temporarily unavailable", e);
        } finally {
            lastCallNanos = System.nanoTime();
        }
    }

    private void waitForRateLimit() {
        if (lastCallNanos == 0) {
            return;
        }
        long waitNanos = lastCallNanos + minInterval.toNanos() - System.nanoTime();
        if (waitNanos > 0) {
            try {
                Thread.sleep(Duration.ofNanos(waitNanos));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GeocodingUnavailableException("Interrupted while waiting for the rate limit", e);
            }
        }
    }

    private static String normalize(String address) {
        return address.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}