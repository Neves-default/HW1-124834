package pt.ua.tqs.tuttibox.bdd;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ua.tqs.tuttibox.adapter.FakeLockerHardware;
import pt.ua.tqs.tuttibox.domain.CompartmentState;
import pt.ua.tqs.tuttibox.domain.ParcelState;
import pt.ua.tqs.tuttibox.domain.Size;
import pt.ua.tqs.tuttibox.exception.TuttiboxException;
import pt.ua.tqs.tuttibox.port.Notification;
import pt.ua.tqs.tuttibox.service.CompartmentService;
import pt.ua.tqs.tuttibox.service.RegistrationResult;
import pt.ua.tqs.tuttibox.service.ReservationService;
import pt.ua.tqs.tuttibox.testdoubles.MutableClock;
import pt.ua.tqs.tuttibox.testdoubles.RecordingNotifier;
import pt.ua.tqs.tuttibox.testdoubles.TestData;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions. They drive the SERVICE layer (not the UI or HTTP): the scenarios are about
 * business rules, and the service layer gives direct control over time (MutableClock) and over
 * hardware failures (FakeLockerHardware). The REST layer is covered by ApiIT, the UI by Playwright.
 */
public class LockerSteps {

    @Autowired
    ReservationService reservations;
    @Autowired
    CompartmentService compartments;
    @Autowired
    TestData data;
    @Autowired
    FakeLockerHardware hardware;
    @Autowired
    RecordingNotifier notifier;
    @Autowired
    MutableClock clock;

    private final Map<String, Long> stations = new HashMap<>();
    private String trackingCode;
    private Long originalCompartment;
    private RegistrationResult registration;
    private String answer;
    private String rejection;

    @Before
    public void resetWorld() {
        data.reset();
        hardware.reset();
        notifier.clear();
        clock.set(MutableClock.START);
    }

    // ---------- Given ----------

    @Given("a station {string} with compartments {string}")
    public void aStation(String name, String sizes) {
        Size[] list = Arrays.stream(sizes.split(",")).map(String::trim).map(Size::valueOf).toArray(Size[]::new);
        stations.put(name, data.station(name, 40.6439, -8.6403, list));
    }

    @Given("a parcel of size {string} is registered at {string}")
    public void aParcelIsRegistered(String size, String station) {
        trackingCode = reservations.register(stations.get(station), Size.valueOf(size), "ana@example.com").trackingCode();
    }

    @Given("a parcel of size {string} is already registered at {string}")
    public void anotherParcelIsRegistered(String size, String station) {
        reservations.register(stations.get(station), Size.valueOf(size), "other@example.com");
    }

    @Given("a parcel of size {string} is ready for pickup at {string}")
    public void aParcelIsReady(String size, String station) {
        aParcelIsRegistered(size, station);
        assertThat(reservations.dropOff(stations.get(station), trackingCode).isOk()).isTrue();
    }

    @Given("the door of the parcel's compartment fails to open")
    public void doorFails() {
        originalCompartment = data.parcelCompartmentId(trackingCode);
        hardware.failToOpen(originalCompartment);
    }

    @Given("the recipient has already collected the parcel at {string}")
    public void alreadyCollected(String station) {
        enterCorrectCode(station);
        assertThat(answer).isEqualTo("OK");
    }

    // ---------- When ----------

    @When("the partner registers a parcel of size {string} for {string}")
    public void partnerRegisters(String size, String station) {
        attempt(() -> registration = reservations.register(stations.get(station), Size.valueOf(size), "ana@example.com"));
        if (registration != null) {
            trackingCode = registration.trackingCode();
        }
    }

    @When("the courier drops off the parcel at {string}")
    public void courierDropsOff(String station) {
        answer = terminal(() -> reservations.dropOff(stations.get(station), trackingCode).status().name());
    }

    @When("the recipient enters the correct pickup code at {string}")
    public void enterCorrectCode(String station) {
        String code = notifier.pickupCodeFor(trackingCode).orElseThrow();
        answer = terminal(() -> reservations.pickup(stations.get(station), code).status().name());
    }

    @When("a wrong pickup code is entered {int} times at {string}")
    public void wrongCodes(int times, String station) {
        String code = notifier.pickupCodeFor(trackingCode).orElseThrow();
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < times; i++) {
            answer = terminal(() -> reservations.pickup(stations.get(station), wrong).status().name());
        }
    }

    @When("{int} day(s) and {int} minute(s) pass")
    public void daysPass(int days, int minutes) {
        clock.advance(Duration.ofDays(days).plusMinutes(minutes));
    }

    @When("{int} minute(s) and {int} second(s) pass")
    public void minutesPass(int minutes, int seconds) {
        clock.advance(Duration.ofMinutes(minutes).plusSeconds(seconds));
    }

    @When("the deadline check runs")
    public void deadlineCheck() {
        reservations.applyDeadlines();
    }

    @When("the operator registers the removal of the parcel")
    public void operatorRemoves() {
        attempt(() -> reservations.registerRemoval(trackingCode));
    }

    @When("the operator puts the parcel's compartment out of service")
    public void operatorOutOfService() {
        attempt(() -> compartments.putOutOfService(data.parcelCompartmentId(trackingCode)));
    }

    // ---------- Then ----------

    @Then("the registration succeeds with state {string}")
    public void registrationSucceeds(String state) {
        assertThat(rejection).isNull();
        assertThat(registration.state()).isEqualTo(ParcelState.valueOf(state));
    }

    @Then("the parcel is assigned a compartment of size {string}")
    public void assignedSize(String size) {
        assertThat(registration.compartmentSize()).isEqualTo(Size.valueOf(size));
    }

    @Then("the registration/operation is rejected with {string}")
    public void rejected(String code) {
        assertThat(rejection).isEqualTo(code);
    }

    @Then("the terminal answers {string}")
    public void terminalAnswers(String expected) {
        assertThat(answer).isEqualTo(expected);
    }

    @Then("the parcel state is {string}")
    public void parcelState(String state) {
        assertThat(data.parcelState(trackingCode)).isEqualTo(ParcelState.valueOf(state));
    }

    @Then("the parcel's compartment is {string}")
    public void parcelCompartmentState(String state) {
        assertThat(data.compartmentState(data.parcelCompartmentId(trackingCode))).isEqualTo(CompartmentState.valueOf(state));
    }

    @Then("the original compartment is {string}")
    public void originalCompartmentState(String state) {
        assertThat(data.compartmentState(originalCompartment)).isEqualTo(CompartmentState.valueOf(state));
    }

    @Then("the parcel is in a different compartment")
    public void differentCompartment() {
        assertThat(data.parcelCompartmentId(trackingCode)).isNotNull().isNotEqualTo(originalCompartment);
    }

    @Then("the recipient receives a pickup code")
    public void recipientReceivesCode() {
        assertThat(notifier.pickupCodeFor(trackingCode)).hasValueSatisfying(c -> assertThat(c).matches("\\d{6}"));
    }

    @Then("the operator is notified")
    public void operatorNotified() {
        assertThat(notifier.sent(Notification.Type.OPERATOR_ALERT)).isNotEmpty();
    }

    // ---------- helpers ----------

    private void attempt(Runnable action) {
        rejection = null;
        try {
            action.run();
        } catch (TuttiboxException e) {
            rejection = e.getCode();
        }
    }

    private static String terminal(Supplier<String> action) {
        try {
            return action.get();
        } catch (TuttiboxException e) {
            return e.getCode();
        }
    }
}
