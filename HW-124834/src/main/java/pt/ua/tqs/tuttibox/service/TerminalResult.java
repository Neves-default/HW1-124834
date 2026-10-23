package pt.ua.tqs.tuttibox.service;

/** Outcome of an operation at the locker terminal. */
public record TerminalResult(Status status, String message, String compartmentCode) {

    public enum Status { OK, INVALID_CODE, HARDWARE_FAILURE }

    public static TerminalResult ok(String message, String compartmentCode) {
        return new TerminalResult(Status.OK, message, compartmentCode);
    }

    public static TerminalResult invalidCode(String message) {
        return new TerminalResult(Status.INVALID_CODE, message, null);
    }

    public static TerminalResult hardwareFailure(String message) {
        return new TerminalResult(Status.HARDWARE_FAILURE, message, null);
    }

    public boolean isOk() {
        return status == Status.OK;
    }
}
