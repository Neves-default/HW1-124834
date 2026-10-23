package pt.ua.tqs.tuttibox.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Protection against guessing: 5 consecutive wrong codes block the terminal for 2 minutes. */
class TerminalLockoutTest {

    private static final int MAX = 5;
    private static final Duration LOCKOUT = Duration.ofMinutes(2);
    private final Instant now = Instant.parse("2026-10-20T10:00:00Z");

    @Test
    void fourWrongCodesDoNotBlock() {
        TerminalLockout lockout = new TerminalLockout();
        for (int i = 0; i < 4; i++) {
            lockout.registerFailure(now, MAX, LOCKOUT);
        }
        assertThat(lockout.isBlocked(now)).isFalse();
        assertThat(lockout.getFailedAttempts()).isEqualTo(4);
    }

    @Test
    void fifthWrongCodeBlocksForTwoMinutes() {
        TerminalLockout lockout = new TerminalLockout();
        for (int i = 0; i < 5; i++) {
            lockout.registerFailure(now, MAX, LOCKOUT);
        }
        assertThat(lockout.isBlocked(now)).isTrue();
        assertThat(lockout.isBlocked(now.plus(LOCKOUT).minusSeconds(1))).isTrue();
        assertThat(lockout.isBlocked(now.plus(LOCKOUT))).isFalse();
    }

    @Test
    void aCorrectCodeResetsTheCounter() {
        TerminalLockout lockout = new TerminalLockout();
        for (int i = 0; i < 4; i++) {
            lockout.registerFailure(now, MAX, LOCKOUT);
        }
        lockout.reset();
        lockout.registerFailure(now, MAX, LOCKOUT);

        assertThat(lockout.isBlocked(now)).isFalse();
        assertThat(lockout.getFailedAttempts()).isEqualTo(1);
    }
}
