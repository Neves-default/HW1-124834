package pt.ua.tqs.tuttibox.testdoubles;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock that tests move forward, so time rules are tested without waiting or sleeping. */
public class MutableClock extends Clock {

    public static final Instant START = Instant.parse("2026-10-20T10:00:00Z");

    private volatile Instant now;
    private final ZoneId zone;

    public MutableClock() {
        this(START, ZoneOffset.UTC);
    }

    public MutableClock(Instant start, ZoneId zone) {
        this.now = start;
        this.zone = zone;
    }

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    public void set(Instant instant) {
        now = instant;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(now, zone);
    }

    @Override
    public Instant instant() {
        return now;
    }
}
