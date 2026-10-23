package pt.ua.tqs.tuttibox.testdoubles;

import pt.ua.tqs.tuttibox.port.Notification;
import pt.ua.tqs.tuttibox.port.NotificationPort;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fake notification channel: keeps every notification in memory so tests can read them. */
public class RecordingNotifier implements NotificationPort {

    private static final Pattern CODE = Pattern.compile("Pickup code: (\\d+)");

    private final List<Notification> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(Notification notification) {
        sent.add(notification);
    }

    public List<Notification> sent() {
        return List.copyOf(sent);
    }

    public List<Notification> sent(Notification.Type type) {
        return sent.stream().filter(n -> n.type() == type).toList();
    }

    /** The pickup code the recipient received for a parcel (the "SMS" content). */
    public Optional<String> pickupCodeFor(String trackingCode) {
        return sent.stream()
                .filter(n -> n.type() == Notification.Type.PICKUP_CODE && trackingCode.equals(n.trackingCode()))
                .map(n -> CODE.matcher(n.message()))
                .filter(Matcher::find)
                .map(m -> m.group(1))
                .reduce((first, second) -> second);
    }

    public void clear() {
        sent.clear();
    }
}
