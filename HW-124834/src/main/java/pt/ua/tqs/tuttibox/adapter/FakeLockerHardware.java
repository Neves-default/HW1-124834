package pt.ua.tqs.tuttibox.adapter;

import org.springframework.stereotype.Component;
import pt.ua.tqs.tuttibox.port.LockerHardwarePort;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory fake of the locker hardware (real devices are out of scope).
 * <ul>
 *   <li><b>Demo mode</b> (default): behaves like a working locker. Doors open on request and are
 *       reported closed right away, as if the user closed them.</li>
 *   <li><b>Failure injection</b> (tests): {@link #failToOpen(long)} and {@link #neverCloses(long)}
 *       make a given compartment misbehave.</li>
 * </ul>
 * State kept: the last known door state of each compartment.
 */
@Component
public class FakeLockerHardware implements LockerHardwarePort {

    public enum Door { OPEN, CLOSED }

    private final Map<Long, Door> doors = new ConcurrentHashMap<>();
    private final Set<Long> failToOpen = ConcurrentHashMap.newKeySet();
    private final Set<Long> neverCloses = ConcurrentHashMap.newKeySet();

    @Override
    public boolean openDoor(long compartmentId) {
        if (failToOpen.contains(compartmentId)) {
            return false;
        }
        doors.put(compartmentId, Door.OPEN);
        return true;
    }

    @Override
    public boolean isDoorClosed(long compartmentId) {
        if (neverCloses.contains(compartmentId)) {
            return false;
        }
        doors.put(compartmentId, Door.CLOSED); // simulates the user closing the door
        return true;
    }

    // ---- failure injection ----

    public void failToOpen(long compartmentId) {
        failToOpen.add(compartmentId);
    }

    public void neverCloses(long compartmentId) {
        neverCloses.add(compartmentId);
    }

    public void reset() {
        doors.clear();
        failToOpen.clear();
        neverCloses.clear();
    }

    public Optional<Door> door(long compartmentId) {
        return Optional.ofNullable(doors.get(compartmentId));
    }
}
