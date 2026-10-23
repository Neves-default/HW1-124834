package pt.ua.tqs.tuttibox.domain;

public enum ParcelState {
    CREATED, READY_FOR_PICKUP, PICKED_UP, CANCELLED, EXPIRED, RETURNED;

    public boolean isFinal() {
        return this == PICKED_UP || this == CANCELLED || this == RETURNED;
    }
}
