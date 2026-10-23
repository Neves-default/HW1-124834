package pt.ua.tqs.tuttibox.exception;

/** The request is valid but conflicts with the current state of a resource (HTTP 409). */
public class ConflictException extends TuttiboxException {
    public ConflictException(String code, String message) {
        super(409, code, message);
    }
}
