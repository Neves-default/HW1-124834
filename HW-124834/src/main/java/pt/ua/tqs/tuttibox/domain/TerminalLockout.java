package pt.ua.tqs.tuttibox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Duration;
import java.time.Instant;

/** Protection against guessing pickup codes: N consecutive wrong codes block the terminal for a while. */
@Embeddable
public class TerminalLockout {

    @Column(name = "failed_pickup_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "pickup_blocked_until")
    private Instant blockedUntil;

    public boolean isBlocked(Instant now) {
        return blockedUntil != null && now.isBefore(blockedUntil);
    }

    public void registerFailure(Instant now, int maxAttempts, Duration lockout) {
        failedAttempts++;
        if (failedAttempts >= maxAttempts) {
            blockedUntil = now.plus(lockout);
            failedAttempts = 0;
        }
    }

    public void reset() {
        failedAttempts = 0;
        blockedUntil = null;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public Instant getBlockedUntil() {
        return blockedUntil;
    }
}
