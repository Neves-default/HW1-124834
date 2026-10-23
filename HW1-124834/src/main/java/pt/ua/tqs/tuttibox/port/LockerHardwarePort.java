package pt.ua.tqs.tuttibox.port;

/**
 * The minimum the business logic needs from the locker hardware. Synchronous:
 * the backend calls and waits for the answer.
 */
public interface LockerHardwarePort {

    /** @return true if the door of the compartment opened */
    boolean openDoor(long compartmentId);

    /** @return true if the door of the compartment is confirmed closed */
    boolean isDoorClosed(long compartmentId);
}
