package pt.ua.tqs.tuttibox.service;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.ua.tqs.tuttibox.testdoubles.TestProperties;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PickupCodeServiceTest {

    private final PickupCodeService service = new PickupCodeService(TestProperties.defaults());

    @RepeatedTest(20)
    void codesHaveSixDigits() {
        assertThat(service.newCode()).matches("\\d{6}");
    }

    @Test
    void codesAreRandom() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            codes.add(service.newCode());
        }
        assertThat(codes).hasSizeGreaterThan(190); // collisions are possible but rare
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "12345", "1234567", "12a456", " 123456"})
    void malformedCodesAreDetected(String code) {
        assertThat(service.isWellFormed(code)).isFalse();
    }

    @Test
    void hashNeverContainsThePlainCodeAndDependsOnTheStation() {
        String hash1 = service.hash(1, "123456");

        assertThat(hash1).doesNotContain("123456").hasSize(64);
        assertThat(service.hash(1, "123456")).isEqualTo(hash1);
        assertThat(service.hash(2, "123456")).isNotEqualTo(hash1);
    }
}
