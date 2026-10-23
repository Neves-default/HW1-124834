package pt.ua.tqs.tuttibox.exception;

import pt.ua.tqs.tuttibox.domain.ParcelEvent;
import pt.ua.tqs.tuttibox.domain.ParcelState;

public class InvalidTransitionException extends ConflictException {
    public InvalidTransitionException(ParcelState from, ParcelEvent event) {
        super("INVALID_TRANSITION", "Event " + event + " is not allowed when the parcel is " + from);
    }
}
