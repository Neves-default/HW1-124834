package pt.ua.tqs.tuttibox.service;

import pt.ua.tqs.tuttibox.domain.ParcelState;
import pt.ua.tqs.tuttibox.domain.Size;
import pt.ua.tqs.tuttibox.model.Parcel;

import java.util.List;

/** What the recipient / partner can see. It never includes the pickup code. */
public record TrackingView(String trackingCode, ParcelState state, String stationName, Size size,
                           List<TransitionView> history) {

    static TrackingView of(Parcel p) {
        return new TrackingView(p.getTrackingCode(), p.getState(), p.getStation().getName(), p.getSize(),
                p.getHistory().stream().map(TransitionView::of).toList());
    }
}
