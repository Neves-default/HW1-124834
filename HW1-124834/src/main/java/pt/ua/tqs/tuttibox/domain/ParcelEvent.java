package pt.ua.tqs.tuttibox.domain;

/** Events that move a parcel through its lifecycle. */
public enum ParcelEvent {
    DEPOSIT,            // courier deposits the parcel and the door is confirmed closed
    CANCEL,             // partner cancels the reservation
    DROP_OFF_DEADLINE,  // system: not deposited in time
    PICKUP,             // recipient enters a valid code and the door opens
    PICKUP_DEADLINE,    // system: not collected in time
    REMOVE              // operator removes an expired parcel
}
