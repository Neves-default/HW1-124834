package pt.ua.tqs.tuttibox.domain;

import org.junit.jupiter.api.Test;
import pt.ua.tqs.tuttibox.exception.InvalidTransitionException;
import pt.ua.tqs.tuttibox.model.Compartment;
import pt.ua.tqs.tuttibox.model.Parcel;
import pt.ua.tqs.tuttibox.model.Station;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParcelTest {

    private final Instant t0 = Instant.parse("2026-10-20T10:00:00Z");
    private final Station station = new Station("S", "addr", 40.6, -8.6);

    private Parcel newParcel() {
        return Parcel.register("TBX-TEST0001", station, new Compartment(station, "M1", Size.M), Size.M, "ana@example.com", t0);
    }

    @Test
    void registrationRecordsTheFirstTransitionByThePartner() {
        Parcel p = newParcel();

        assertThat(p.getState()).isEqualTo(ParcelState.CREATED);
        assertThat(p.getHistory()).singleElement().satisfies(t -> {
            assertThat(t.getFromState()).isNull();
            assertThat(t.getToState()).isEqualTo(ParcelState.CREATED);
            assertThat(t.getActor()).isEqualTo(Actor.PARTNER);
            assertThat(t.getOccurredAt()).isEqualTo(t0);
        });
    }

    @Test
    void everyTransitionIsRecordedWithTimestampAndActor() {
        Parcel p = newParcel();
        Instant t1 = t0.plusSeconds(3600);
        Instant t2 = t1.plusSeconds(3600);

        p.apply(ParcelEvent.DEPOSIT, Actor.COURIER, t1);
        p.apply(ParcelEvent.PICKUP, Actor.RECIPIENT, t2);

        assertThat(p.getHistory()).hasSize(3);
        assertThat(p.getHistory().get(1).getActor()).isEqualTo(Actor.COURIER);
        assertThat(p.getHistory().get(1).getOccurredAt()).isEqualTo(t1);
        assertThat(p.getHistory().get(2).getToState()).isEqualTo(ParcelState.PICKED_UP);
        assertThat(p.getReadyAt()).isEqualTo(t1);
    }

    @Test
    void leavingReadyForPickupInvalidatesThePickupCode() {
        Parcel p = newParcel();
        p.apply(ParcelEvent.DEPOSIT, Actor.COURIER, t0);
        p.setPickupCodeHash("hash");

        p.apply(ParcelEvent.PICKUP_DEADLINE, Actor.SYSTEM, t0.plusSeconds(10));

        assertThat(p.getState()).isEqualTo(ParcelState.EXPIRED);
        assertThat(p.getPickupCodeHash()).isNull();
    }

    @Test
    void anInvalidEventDoesNotChangeTheParcel() {
        Parcel p = newParcel();

        assertThatThrownBy(() -> p.apply(ParcelEvent.PICKUP, Actor.RECIPIENT, t0))
                .isInstanceOf(InvalidTransitionException.class);
        assertThat(p.getState()).isEqualTo(ParcelState.CREATED);
        assertThat(p.getHistory()).hasSize(1);
    }
}
