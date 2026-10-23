package pt.ua.tqs.tuttibox.port;

public record Notification(Type type, String to, String trackingCode, String message) {

    public enum Type {
        /** To the recipient: the parcel is ready and this is the pickup code. */
        PICKUP_CODE,
        /** To the operator: a compartment needs attention. */
        OPERATOR_ALERT
    }
}
