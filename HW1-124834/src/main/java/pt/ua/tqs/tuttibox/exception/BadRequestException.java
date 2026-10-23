package pt.ua.tqs.tuttibox.exception;

public class BadRequestException extends TuttiboxException {
    public BadRequestException(String code, String message) {
        super(400, code, message);
    }
}
