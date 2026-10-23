package pt.ua.tqs.tuttibox.exception;

public class NotFoundException extends TuttiboxException {
    public NotFoundException(String code, String message) {
        super(404, code, message);
    }
}
