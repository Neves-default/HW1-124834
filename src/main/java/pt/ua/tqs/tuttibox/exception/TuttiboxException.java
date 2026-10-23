package pt.ua.tqs.tuttibox.exception;

/** Base class for business errors. Carries the HTTP status and a stable error code for the API. */
public class TuttiboxException extends RuntimeException {

    private final int status;
    private final String code;

    public TuttiboxException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public TuttiboxException(int status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
