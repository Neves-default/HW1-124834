package pt.ua.tqs.tuttibox.domain;

import java.util.Arrays;
import java.util.List;

/** Compartment / parcel sizes, ordered from smallest to largest. */
public enum Size {
    S, M, L;

    /** True if a parcel of this size fits in a compartment of the given size. */
    public boolean fitsIn(Size compartment) {
        return compartment.ordinal() >= ordinal();
    }

    /**
     * Compartment sizes to try, in order, for a parcel of this size:
     * same size first and, if allowed, the next larger ones (S -> M -> L). Never smaller.
     */
    public List<Size> allocationOrder(boolean allowLarger) {
        if (!allowLarger) {
            return List.of(this);
        }
        return Arrays.stream(values()).filter(this::fitsIn).toList();
    }
}
