package pt.ua.tqs.tuttibox.port;

/** Sends notifications (SMS / e-mail in a real system). */
public interface NotificationPort {

    void send(Notification notification);
}
