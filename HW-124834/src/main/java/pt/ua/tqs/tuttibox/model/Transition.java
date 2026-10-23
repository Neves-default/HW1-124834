package pt.ua.tqs.tuttibox.model;

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
import jakarta.persistence.Table;
import pt.ua.tqs.tuttibox.domain.Actor;
import pt.ua.tqs.tuttibox.domain.ParcelState;

import java.time.Instant;

/** One entry of a parcel's history: from-state, to-state, who did it and when. */
@Entity
@Table(name = "transitions")
public class Transition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parcel_id")
    private Parcel parcel;

    @Enumerated(EnumType.STRING)
    private ParcelState fromState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParcelState toState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Actor actor;

    @Column(nullable = false)
    private Instant occurredAt;

    protected Transition() {
    }

    public Transition(Parcel parcel, ParcelState fromState, ParcelState toState, Actor actor, Instant occurredAt) {
        this.parcel = parcel;
        this.fromState = fromState;
        this.toState = toState;
        this.actor = actor;
        this.occurredAt = occurredAt;
    }

    public ParcelState getFromState() {
        return fromState;
    }

    public ParcelState getToState() {
        return toState;
    }

    public Actor getActor() {
        return actor;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
