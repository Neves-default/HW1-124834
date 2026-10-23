package pt.ua.tqs.tuttibox.service;

import pt.ua.tqs.tuttibox.domain.ParcelState;
import pt.ua.tqs.tuttibox.domain.Size;
import pt.ua.tqs.tuttibox.model.Parcel;

import java.time.Instant;

/** Parcel line on the operator page. */
public record ParcelSummary(String trackingCode, ParcelState state, Size size, String compartmentCode,
                            Instant createdAt, Instant readyAt) {

    static ParcelSummary of(Parcel p) {
        return new ParcelSummary(p.getTrackingCode(), p.getState(), p.getSize(),
                p.getCompartment() == null ? null : p.getCompartment().getCode(),
                p.getCreatedAt(), p.getReadyAt());
    }
}
