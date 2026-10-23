package pt.ua.tqs.tuttibox.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Compartment allocation rule: same size first, then larger, never smaller. */
class SizeTest {

    @ParameterizedTest(name = "parcel {0} fits in compartment {1}: {2}")
    @CsvSource({
            "S, S, true", "S, M, true", "S, L, true",
            "M, S, false", "M, M, true", "M, L, true",
            "L, S, false", "L, M, false", "L, L, true"})
    void fitsOnlyInSameOrLargerCompartments(Size parcel, Size compartment, boolean fits) {
        assertThat(parcel.fitsIn(compartment)).isEqualTo(fits);
    }

    @ParameterizedTest(name = "parcel {0}, larger allowed={1} -> {2}")
    @CsvSource({
            "S, true, S M L",
            "M, true, M L",
            "L, true, L",
            "S, false, S",
            "M, false, M"})
    void allocationOrderStartsWithTheSameSize(Size parcel, boolean allowLarger, String expected) {
        List<Size> expectedOrder = Arrays.stream(expected.split(" ")).map(Size::valueOf).toList();
        assertThat(parcel.allocationOrder(allowLarger)).containsExactlyElementsOf(expectedOrder);
    }
}
