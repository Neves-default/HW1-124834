package pt.ua.tqs.tuttibox.testdoubles;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import pt.ua.tqs.tuttibox.repository.CompartmentRepository;
import pt.ua.tqs.tuttibox.repository.ParcelRepository;
import pt.ua.tqs.tuttibox.repository.StationRepository;

/**
 * Test doubles plugged into the ports for Spring-based tests:
 * controllable clock, recording notifier, fake geocoder. The hardware port uses the
 * application's FakeLockerHardware, configured per test to inject failures.
 */
@TestConfiguration
public class TestDoublesConfig {

    @Bean
    @Primary
    public MutableClock testClock() {
        return new MutableClock();
    }

    @Bean
    @Primary
    public RecordingNotifier recordingNotifier() {
        return new RecordingNotifier();
    }

    @Bean
    @Primary
    public FakeGeocoder fakeGeocoder() {
        return new FakeGeocoder();
    }

    @Bean
    public TestData testData(StationRepository stations, CompartmentRepository compartments, ParcelRepository parcels,
                             PlatformTransactionManager transactionManager) {
        return new TestData(stations, compartments, parcels, transactionManager);
    }
}
