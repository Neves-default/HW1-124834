package pt.ua.tqs.tuttibox.domain;

import pt.ua.tqs.tuttibox.exception.InvalidTransitionException;

import java.util.Map;

import static pt.ua.tqs.tuttibox.domain.ParcelEvent.*;
import static pt.ua.tqs.tuttibox.domain.ParcelState.*;

/**
 * The parcel state machine as a transition table: (current state, event) -> new state.
 * Any pair that is not in the table is rejected.
 */
public final class ParcelLifecycle {

    private static final Map<ParcelState, Map<ParcelEvent, ParcelState>> TRANSITIONS = Map.of(
            CREATED, Map.of(
                    DEPOSIT, READY_FOR_PICKUP,
                    CANCEL, CANCELLED,
                    DROP_OFF_DEADLINE, CANCELLED),
            READY_FOR_PICKUP, Map.of(
                    PICKUP, PICKED_UP,
                    PICKUP_DEADLINE, EXPIRED),
            EXPIRED, Map.of(
                    REMOVE, RETURNED));

    private ParcelLifecycle() {
    }

    /** @throws InvalidTransitionException if the event is not valid in the given state */
    public static ParcelState next(ParcelState from, ParcelEvent event) {
        ParcelState to = TRANSITIONS.getOrDefault(from, Map.of()).get(event);
        if (to == null) {
            throw new InvalidTransitionException(from, event);
        }
        return to;
    }

    public static boolean isAllowed(ParcelState from, ParcelEvent event) {
        return TRANSITIONS.getOrDefault(from, Map.of()).containsKey(event);
    }
}
