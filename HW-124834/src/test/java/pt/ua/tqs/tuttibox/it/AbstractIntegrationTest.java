package pt.ua.tqs.tuttibox.it;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import pt.ua.tqs.tuttibox.adapter.FakeLockerHardware;
import pt.ua.tqs.tuttibox.testdoubles.FakeGeocoder;
import pt.ua.tqs.tuttibox.testdoubles.MutableClock;
import pt.ua.tqs.tuttibox.testdoubles.RecordingNotifier;
import pt.ua.tqs.tuttibox.testdoubles.TestData;
import pt.ua.tqs.tuttibox.testdoubles.TestDoublesConfig;

/**
 * Full Spring context against a real PostgreSQL started by Testcontainers (see application-test.yml),
 * with test doubles on the ports. All integration test classes share this context.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestDoublesConfig.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected TestData data;
    @Autowired
    protected FakeLockerHardware hardware;
    @Autowired
    protected RecordingNotifier notifier;
    @Autowired
    protected MutableClock clock;
    @Autowired
    protected FakeGeocoder geocoder;

    @BeforeEach
    void resetWorld() {
        data.reset();
        hardware.reset();
        notifier.clear();
        clock.set(MutableClock.START);
        geocoder.reset();
    }
}
