package pt.ua.tqs.tuttibox.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pt.ua.tqs.tuttibox.port.Notification;
import pt.ua.tqs.tuttibox.port.NotificationPort;

/**
 * Real SMS/e-mail is out of scope: notifications are written to a dedicated log channel
 * ("tuttibox.notifications"), which plays the role of the SMS gateway.
 */
@Component
public class LogNotifier implements NotificationPort {

    private static final Logger CHANNEL = LoggerFactory.getLogger("tuttibox.notifications");

    @Override
    public void send(Notification notification) {
        CHANNEL.info("[{}] to={} parcel={} : {}", notification.type(), notification.to(),
                notification.trackingCode(), notification.message());
    }
}
