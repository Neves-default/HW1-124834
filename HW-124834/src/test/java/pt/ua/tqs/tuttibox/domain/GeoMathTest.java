package pt.ua.tqs.tuttibox.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeoMathTest {

    @Test
    void samePointIsZeroKilometresAway() {
        Coordinates p = new Coordinates(40.6405, -8.6538);
        assertThat(GeoMath.distanceKm(p, p)).isZero();
    }

    @Test
    void oneDegreeOfLongitudeOnTheEquatorIsAbout111Km() {
        double d = GeoMath.distanceKm(new Coordinates(0, 0), new Coordinates(0, 1));
        assertThat(d).isCloseTo(111.19, within(0.05));
    }

    @Test
    void lisbonToPortoIsAbout274Km() {
        double d = GeoMath.distanceKm(new Coordinates(38.7223, -9.1393), new Coordinates(41.1579, -8.6291));
        assertThat(d).isCloseTo(274, within(2.0));
    }

    @Test
    void distanceIsSymmetric() {
        Coordinates a = new Coordinates(40.6439, -8.6403);
        Coordinates b = new Coordinates(40.6303, -8.6575);
        assertThat(GeoMath.distanceKm(a, b)).isEqualTo(GeoMath.distanceKm(b, a));
    }
}
