package pt.ua.tqs.tuttibox.domain;

/** Distance between two points on Earth (haversine formula). */
public final class GeoMath {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private GeoMath() {
    }

    public static double distanceKm(Coordinates a, Coordinates b) {
        double lat1 = Math.toRadians(a.latitude());
        double lat2 = Math.toRadians(b.latitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(h)));
    }
}
