package pt.ua.tqs.tuttibox.service;

/** A station with its free compartments per size. distanceKm is null when not computed. */
public record StationView(Long id, String name, String address, Double distanceKm,
                          long freeS, long freeM, long freeL) {
}
