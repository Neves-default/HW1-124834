package pt.ua.tqs.tuttibox.it;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ua.tqs.tuttibox.domain.Actor;
import pt.ua.tqs.tuttibox.domain.CompartmentState;
import pt.ua.tqs.tuttibox.domain.ParcelState;
import pt.ua.tqs.tuttibox.domain.Size;
import pt.ua.tqs.tuttibox.exception.BadRequestException;
import pt.ua.tqs.tuttibox.exception.ConflictException;
import pt.ua.tqs.tuttibox.exception.InvalidTransitionException;
import pt.ua.tqs.tuttibox.exception.TerminalBlockedException;
import pt.ua.tqs.tuttibox.port.Notification;
import pt.ua.tqs.tuttibox.service.CompartmentService;
import pt.ua.tqs.tuttibox.service.RegistrationResult;
import pt.ua.tqs.tuttibox.service.ReservationService;
import pt.ua.tqs.tuttibox.service.TerminalResult;
import pt.ua.tqs.tuttibox.service.TrackingView;
import pt.ua.tqs.tuttibox.service.TransitionView;
import pt.ua.tqs.tuttibox.testdoubles.MutableClock;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Business rules through the service layer with the real database, the fake hardware (failure injection)
 * and the controllable clock (deadlines without waiting).
 */
class ReservationServiceIT extends AbstractIntegrationTest {

    @Autowired
    ReservationService reservations;
    @Autowired
    CompartmentService compartments;

    private Long station(Size... sizes) {
        return data.station("Test station", 40.64, -8.65, sizes);
    }

    private String readyParcel(Long stationId) {
        String tracking = reservations.register(stationId, Size.M, "ana@example.com").trackingCode();
        assertThat(reservations.dropOff(stationId, tracking).isOk()).isTrue();
        return tracking;
    }

    @Nested
    class Registration {

        @Test
        void reservesACompartmentOfTheSameSize() {
            Long st = station(Size.S, Size.M, Size.L);

            RegistrationResult r = reservations.register(st, Size.M, "ana@example.com");

            assertThat(r.state()).isEqualTo(ParcelState.CREATED);
            assertThat(r.compartmentSize()).isEqualTo(Size.M);
            assertThat(r.trackingCode()).matches("TBX-[A-Z0-9]{8}");
            assertThat(data.compartmentState(data.parcelCompartmentId(r.trackingCode()))).isEqualTo(CompartmentState.RESERVED);
        }

        @Test
        void usesTheNextLargerSizeWhenTheExactSizeIsFull() {
            Long st = station(Size.S, Size.L);
            reservations.register(st, Size.S, "a@example.com");

            RegistrationResult r = reservations.register(st, Size.S, "b@example.com");

            assertThat(r.compartmentSize()).isEqualTo(Size.L);
        }

        @Test
        void neverUsesASmallerCompartment() {
            Long st = station(Size.S, Size.M);

            assertThatThrownBy(() -> reservations.register(st, Size.L, "a@example.com"))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("no free compartment");
        }

        @Test
        void unknownStationIsABadRequest() {
            assertThatThrownBy(() -> reservations.register(999_999L, Size.S, "a@example.com"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        void partnerCanCancelOnlyBeforeDropOff() {
            Long st = station(Size.M, Size.M);
            String created = reservations.register(st, Size.M, "a@example.com").trackingCode();
            String ready = readyParcel(st);

            reservations.cancel(created);

            assertThat(data.parcelState(created)).isEqualTo(ParcelState.CANCELLED);
            assertThat(data.compartmentState(data.parcelCompartmentId(created))).isEqualTo(CompartmentState.FREE);
            assertThatThrownBy(() -> reservations.cancel(ready)).isInstanceOf(InvalidTransitionException.class);
        }
    }

    @Nested
    class DropOff {

        @Test
        void depositingMakesTheParcelReadyAndSendsAPickupCode() {
            Long st = station(Size.M);
            String tracking = reservations.register(st, Size.M, "ana@example.com").trackingCode();

            TerminalResult r = reservations.dropOff(st, tracking);

            assertThat(r.isOk()).isTrue();
            assertThat(data.parcelState(tracking)).isEqualTo(ParcelState.READY_FOR_PICKUP);
            assertThat(data.compartmentState(data.parcelCompartmentId(tracking))).isEqualTo(CompartmentState.OCCUPIED);
            assertThat(notifier.pickupCodeFor(tracking)).hasValueSatisfying(code -> assertThat(code).matches("\\d{6}"));
        }

        @Test
        void pickupCodeIsStoredOnlyAsAHash() {
            Long st = station(Size.M);
            String tracking = readyParcel(st);
            String code = notifier.pickupCodeFor(tracking).orElseThrow();

            String stored = data.parcelPickupCodeHash(tracking);

            assertThat(stored).isNotBlank().hasSize(64).doesNotContain(code);
        }

        @Test
        void doorFailureMovesTheParcelToAnotherCompartment() {
            Long st = station(Size.M, Size.M);
            String tracking = reservations.register(st, Size.M, "ana@example.com").trackingCode();
            Long first = data.parcelCompartmentId(tracking);
            hardware.failToOpen(first);

            TerminalResult r = reservations.dropOff(st, tracking);

            assertThat(r.isOk()).isTrue();
            assertThat(data.compartmentState(first)).isEqualTo(CompartmentState.OUT_OF_SERVICE);
            assertThat(data.parcelCompartmentId(tracking)).isNotEqualTo(first);
            assertThat(data.parcelState(tracking)).isEqualTo(ParcelState.READY_FOR_PICKUP);
            assertThat(notifier.sent(Notification.Type.OPERATOR_ALERT)).hasSize(1);
        }

        @Test
        void doorFailureWithNoOtherCompartmentKeepsTheParcelCreated() {
            Long st = station(Size.M);
            String tracking = reservations.register(st, Size.M, "ana@example.com").trackingCode();
            hardware.failToOpen(data.parcelCompartmentId(tracking));

            TerminalResult r = reservations.dropOff(st, tracking);

            assertThat(r.status()).isEqualTo(TerminalResult.Status.HARDWARE_FAILURE);
            assertThat(data.parcelState(tracking)).isEqualTo(ParcelState.CREATED);
            assertThat(notifier.sent(Notification.Type.OPERATOR_ALERT)).isNotEmpty();
        }

        @Test
        void doorNotConfirmedClosedDoesNotDepositTheParcel() {
            Long st = station(Size.M);
            String tracking = reservations.register(st, Size.M, "ana@example.com").trackingCode();
            hardware.neverCloses(data.parcelCompartmentId(tracking));

            TerminalResult r = reservations.dropOff(st, tracking);

            assertThat(r.status()).isEqualTo(TerminalResult.Status.HARDWARE_FAILURE);
            assertThat(data.parcelState(tracking)).isEqualTo(ParcelState.CREATED);
            assertThat(notifier.pickupCodeFor(tracking)).isEmpty();
        }

        @Test
        void parcelNotDepositedWithinFiveDaysIsCancelled() {
            Long st = station(Size.M);
            String tracking = reservations.register(st, Size.M, "ana@example.com").trackingCode();

            clock.advance(Duration.ofDays(5).minusMinutes(1));
            reservations.applyDeadlines();
            assertThat(data.parcelState(tracking)).isEqualTo(ParcelState.CREATED);

            clock.advance(Duration.ofMinutes(2));
            reservations.applyDeadlines();
            assertThat(data.parcelState(tracking)).isEqualTo(ParcelState.CANCELLED);
            assertThat(data.compartmentState(data.parcelCompartmentId(tracking))).isEqualTo(CompartmentState.FREE);
        }
    }

    @Nested
    class Pickup {

        @Test
        void correctCodeOpensTheCompartmentAndFreesIt() {
            Long st = station(Size.M);
            String tracking = readyParcel(st);
            String code = notifier.pickupCodeFor(tracking).orElseThrow();

            TerminalResult r = reservations.pickup(st, code);

            assertThat(r.isOk()).isTrue();
            assertThat(data.parcelState(tracking)).isEqualTo(ParcelState.PICKED_UP);
            assertThat(data.compartmentState(data.parcelCompartmentId(tracking))).isEqualTo(CompartmentState.FREE);
        }

        @Test
        void codeIsSingleUse() {
            Long st = station(Size.M);
            String code = notifier.pickupCodeFor(readyParcel(st)).orElseThrow();
            reservations.pickup(st, code);

            assertThat(reservations.pickup(st, code).status()).isEqualTo(TerminalResult.Status.INVALID_CODE);
        }

        @Test
        void fiveWrongCodesBlockTheTerminalForTwoMinutes() {
            Long st = station(Size.M);
            String code = notifier.pickupCodeFor(readyParcel(st)).orElseThrow();
            String wrong = code.equals("000000") ? "111111" : "000000";
            for (int i = 0; i < 5; i++) {
                assertThat(reservations.pickup(st, wrong).status()).isEqualTo(TerminalResult.Status.INVALID_CODE);
            }

            assertThatThrownBy(() -> reservations.pickup(st, code)).isInstanceOf(TerminalBlockedException.class);

            clock.advance(Duration.ofMinutes(2));
            assertThat(reservations.pickup(st, code).isOk()).isTrue();
        }

        @Test
        void parcelNotCollectedWithinFiveDaysExpiresAndTheCodeStopsWorking() {
            Long st = station(Size.M);
            String tracking = readyParcel(st);
            String code = notifier.pickupCodeFor(tracking).orElseThrow();

            clock.advance(Duration.ofDays(5).plusMinutes(1));
            reservations.applyDeadlines();

            assertThat(data.parcelState(tracking)).isEqualTo(ParcelState.EXPIRED);
            assertThat(reservations.pickup(st, code).status()).isEqualTo(TerminalResult.Status.INVALID_CODE);
        }

        @Test
        void doorFailureKeepsTheParcelReadyAndTheCodeValid() {
            Long st = station(Size.M);
            String tracking = readyParcel(st);
            String code = notifier.pickupCodeFor(tracking).orElseThrow();
            Long compartment = data.parcelCompartmentId(tracking);
            hardware.failToOpen(compartment);

            TerminalResult r = reservations.pickup(st, code);

            assertThat(r.status()).isEqualTo(TerminalResult.Status.HARDWARE_FAILURE);
            assertThat(data.parcelState(tracking)).isEqualTo(ParcelState.READY_FOR_PICKUP);
            assertThat(data.compartmentNeedsAttention(compartment)).isTrue();
            assertThat(notifier.sent(Notification.Type.OPERATOR_ALERT)).hasSize(1);
        }
    }

    @Nested
    class OperatorAndTracking {

        @Test
        void removingAnExpiredParcelReturnsItAndFreesTheCompartment() {
            Long st = station(Size.M);
            String tracking = readyParcel(st);
            clock.advance(Duration.ofDays(6));
            reservations.applyDeadlines();

            reservations.registerRemoval(tracking);

            assertThat(data.parcelState(tracking)).isEqualTo(ParcelState.RETURNED);
            assertThat(data.compartmentState(data.parcelCompartmentId(tracking))).isEqualTo(CompartmentState.FREE);
        }

        @Test
        void removingAParcelThatIsNotExpiredIsRejected() {
            Long st = station(Size.M);
            String tracking = readyParcel(st);

            assertThatThrownBy(() -> reservations.registerRemoval(tracking)).isInstanceOf(InvalidTransitionException.class);
        }

        @Test
        void onlyFreeCompartmentsCanBePutOutOfServiceAndReactivated() {
            Long st = station(Size.M, Size.L);
            String tracking = readyParcel(st);
            Long occupied = data.parcelCompartmentId(tracking);
            Long free = data.compartmentId(st, "L1");

            assertThatThrownBy(() -> compartments.putOutOfService(occupied)).isInstanceOf(ConflictException.class);

            compartments.putOutOfService(free);
            assertThat(data.compartmentState(free)).isEqualTo(CompartmentState.OUT_OF_SERVICE);
            assertThatThrownBy(() -> reservations.register(st, Size.L, "x@example.com")).isInstanceOf(ConflictException.class);

            compartments.reactivate(free);
            assertThat(data.compartmentState(free)).isEqualTo(CompartmentState.FREE);
        }

        @Test
        void trackingShowsTheTimestampedHistoryWithActors() {
            Long st = station(Size.M);
            String tracking = readyParcel(st);
            String code = notifier.pickupCodeFor(tracking).orElseThrow();
            clock.advance(Duration.ofHours(3));
            reservations.pickup(st, code);

            TrackingView view = reservations.track(tracking);

            assertThat(view.state()).isEqualTo(ParcelState.PICKED_UP);
            assertThat(view.history()).extracting(TransitionView::actor)
                    .containsExactly(Actor.PARTNER, Actor.COURIER, Actor.RECIPIENT);
            assertThat(view.history().get(2).at()).isEqualTo(MutableClock.START.plus(Duration.ofHours(3)));
        }
    }
}
