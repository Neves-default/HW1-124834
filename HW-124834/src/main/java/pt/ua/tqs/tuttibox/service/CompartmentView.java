package pt.ua.tqs.tuttibox.service;

import pt.ua.tqs.tuttibox.domain.CompartmentState;
import pt.ua.tqs.tuttibox.domain.Size;
import pt.ua.tqs.tuttibox.model.Compartment;

public record CompartmentView(Long id, String code, Size size, CompartmentState state, boolean needsAttention) {

    static CompartmentView of(Compartment c) {
        return new CompartmentView(c.getId(), c.getCode(), c.getSize(), c.getState(), c.isNeedsAttention());
    }
}
