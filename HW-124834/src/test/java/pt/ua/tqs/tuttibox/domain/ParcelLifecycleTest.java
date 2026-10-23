package pt.ua.tqs.tuttibox.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import pt.ua.tqs.tuttibox.exception.InvalidTransitionException;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static pt.ua.tqs.tuttibox.domain.ParcelEvent.*;
import static pt.ua.tqs.tuttibox.domain.ParcelState.*;

/** The whole transition table, tested at unit level: every valid and every invalid (state, event) pair. */
class ParcelLifecycleTest {

    static Stream<Arguments> validTransitions() {
        return Stream.of(
                arguments(CREATED, DEPOSIT, READY_FOR_PICKUP),
                arguments(CREATED, CANCEL, CANCELLED),
                arguments(CREATED, DROP_OFF_DEADLINE, CANCELLED),
                arguments(READY_FOR_PICKUP, PICKUP, PICKED_UP),
                arguments(READY_FOR_PICKUP, PICKUP_DEADLINE, EXPIRED),
                arguments(EXPIRED, REMOVE, RETURNED));
    }

    static Stream<Arguments> invalidTransitions() {
        Set<String> valid = Set.of("CREATED/DEPOSIT", "CREATED/CANCEL", "CREATED/DROP_OFF_DEADLINE",
                "READY_FOR_PICKUP/PICKUP", "READY_FOR_PICKUP/PICKUP_DEADLINE", "EXPIRED/REMOVE");
        return Stream.of(ParcelState.values())
                .flatMap(s -> Stream.of(ParcelEvent.values()).map(e -> arguments(s, e)))
                .filter(a -> !valid.contains(a.get()[0] + "/" + a.get()[1]));
    }

    @ParameterizedTest(name = "{0} + {1} -> {2}")
    @MethodSource("validTransitions")
    void validEventsMoveToTheExpectedState(ParcelState from, ParcelEvent event, ParcelState expected) {
        assertThat(ParcelLifecycle.next(from, event)).isEqualTo(expected);
        assertThat(ParcelLifecycle.isAllowed(from, event)).isTrue();
    }

    @ParameterizedTest(name = "{0} + {1} is rejected")
    @MethodSource("invalidTransitions")
    void invalidEventsAreRejected(ParcelState from, ParcelEvent event) {
        assertThat(ParcelLifecycle.isAllowed(from, event)).isFalse();
        assertThatThrownBy(() -> ParcelLifecycle.next(from, event))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining(from.name());
    }

    @ParameterizedTest
    @EnumSource(value = ParcelState.class, names = {"PICKED_UP", "CANCELLED", "RETURNED"})
    void finalStatesAcceptNoEvent(ParcelState state) {
        assertThat(state.isFinal()).isTrue();
        for (ParcelEvent event : ParcelEvent.values()) {
            assertThat(ParcelLifecycle.isAllowed(state, event)).isFalse();
        }
    }
}
