package pt.ua.tqs.tuttibox.service;

import pt.ua.tqs.tuttibox.domain.Actor;
import pt.ua.tqs.tuttibox.domain.ParcelState;
import pt.ua.tqs.tuttibox.model.Transition;

import java.time.Instant;

public record TransitionView(ParcelState from, ParcelState to, Actor actor, Instant at) {

    static TransitionView of(Transition t) {
        return new TransitionView(t.getFromState(), t.getToState(), t.getActor(), t.getOccurredAt());
    }
}
