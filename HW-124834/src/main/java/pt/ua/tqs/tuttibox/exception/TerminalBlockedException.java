package pt.ua.tqs.tuttibox.exception;

import java.time.Instant;

/** Too many wrong pickup codes at this terminal (HTTP 429). */
public class TerminalBlockedException extends TuttiboxException {
    public TerminalBlockedException(Instant until) {
        super(429, "TERMINAL_BLOCKED", "Too many wrong pickup codes. The terminal is blocked until " + until + ".");
    }
}
