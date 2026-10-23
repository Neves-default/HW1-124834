package pt.ua.tqs.tuttibox.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import pt.ua.tqs.tuttibox.domain.Actor;
import pt.ua.tqs.tuttibox.domain.ParcelEvent;
import pt.ua.tqs.tuttibox.domain.ParcelLifecycle;
import pt.ua.tqs.tuttibox.domain.ParcelState;
import pt.ua.tqs.tuttibox.domain.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "parcels")
public class Parcel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String trackingCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id")
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compartment_id")
    private Compartment compartment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Size size;

    @Column(nullable = false)
    private String recipientContact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParcelState state;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant readyAt;

    /** HMAC of the pickup code. The plain code is never stored. */
    private String pickupCodeHash;

    @OneToMany(mappedBy = "parcel", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Transition> history = new ArrayList<>();

    protected Parcel() {
    }

    /** Creates a parcel in state CREATED and records the first transition (actor: partner). */
    public static Parcel register(String trackingCode, Station station, Compartment compartment,
                                  Size size, String recipientContact, Instant now) {
        Parcel parcel = new Parcel();
        parcel.trackingCode = trackingCode;
        parcel.station = station;
        parcel.compartment = compartment;
        parcel.size = size;
        parcel.recipientContact = recipientContact;
        parcel.state = ParcelState.CREATED;
        parcel.createdAt = now;
        parcel.history.add(new Transition(parcel, null, ParcelState.CREATED, Actor.PARTNER, now));
        return parcel;
    }

    /**
     * Applies an event using the transition table. Records the transition (timestamp + actor).
     *
     * @throws pt.ua.tqs.tuttibox.exception.InvalidTransitionException if the event is not valid now
     */
    public ParcelState apply(ParcelEvent event, Actor actor, Instant now) {
        ParcelState from = state;
        ParcelState to = ParcelLifecycle.next(from, event);
        history.add(new Transition(this, from, to, actor, now));
        state = to;
        if (to == ParcelState.READY_FOR_PICKUP) {
            readyAt = now;
        } else {
            pickupCodeHash = null; // the code only works while READY_FOR_PICKUP
        }
        return to;
    }

    public Long getId() {
        return id;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public Station getStation() {
        return station;
    }

    public Compartment getCompartment() {
        return compartment;
    }

    public void setCompartment(Compartment compartment) {
        this.compartment = compartment;
    }

    public Size getSize() {
        return size;
    }

    public String getRecipientContact() {
        return recipientContact;
    }

    public ParcelState getState() {
        return state;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadyAt() {
        return readyAt;
    }

    public String getPickupCodeHash() {
        return pickupCodeHash;
    }

    public void setPickupCodeHash(String pickupCodeHash) {
        this.pickupCodeHash = pickupCodeHash;
    }

    public List<Transition> getHistory() {
        return Collections.unmodifiableList(history);
    }
}
