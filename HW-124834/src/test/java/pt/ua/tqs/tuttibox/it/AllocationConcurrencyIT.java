package pt.ua.tqs.tuttibox.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ua.tqs.tuttibox.domain.Size;
import pt.ua.tqs.tuttibox.exception.ConflictException;
import pt.ua.tqs.tuttibox.service.RegistrationResult;
import pt.ua.tqs.tuttibox.service.ReservationService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "Two partners must never get the same last compartment."
 * Needs the real database engine: the guarantee comes from PostgreSQL row locks.
 */
class AllocationConcurrencyIT extends AbstractIntegrationTest {

    private static final int PARTNERS = 10;

    @Autowired
    ReservationService reservations;

    @Test
    void onlyOnePartnerGetsTheLastCompartment() throws Exception {
        Long st = data.station("One M left", 40.64, -8.65, Size.M);
        AtomicInteger conflicts = new AtomicInteger();
        List<RegistrationResult> successes = runConcurrently(st, Size.M, conflicts);

        assertThat(successes).hasSize(1);
        assertThat(conflicts.get()).isEqualTo(PARTNERS - 1);
        assertThat(data.parcelCount()).isEqualTo(1);
    }

    @Test
    void concurrentPartnersGetDifferentCompartments() throws Exception {
        Long st = data.station("Three M", 40.64, -8.65, Size.M, Size.M, Size.M);
        AtomicInteger conflicts = new AtomicInteger();
        List<RegistrationResult> successes = runConcurrently(st, Size.M, conflicts);

        assertThat(successes).hasSize(3);
        assertThat(successes).extracting(RegistrationResult::compartmentCode).doesNotHaveDuplicates();
        assertThat(conflicts.get()).isEqualTo(PARTNERS - 3);
    }

    private List<RegistrationResult> runConcurrently(Long stationId, Size size, AtomicInteger conflicts) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(PARTNERS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<RegistrationResult>> futures = new ArrayList<>();
        for (int i = 0; i < PARTNERS; i++) {
            int partner = i;
            Callable<RegistrationResult> task = () -> {
                start.await();
                try {
                    return reservations.register(stationId, size, "partner" + partner + "@example.com");
                } catch (ConflictException e) {
                    conflicts.incrementAndGet();
                    return null;
                }
            };
            futures.add(pool.submit(task));
        }
        start.countDown();
        List<RegistrationResult> successes = new ArrayList<>();
        for (Future<RegistrationResult> f : futures) {
            RegistrationResult r = f.get(30, TimeUnit.SECONDS);
            if (r != null) {
                successes.add(r);
            }
        }
        pool.shutdown();
        return successes;
    }
}
