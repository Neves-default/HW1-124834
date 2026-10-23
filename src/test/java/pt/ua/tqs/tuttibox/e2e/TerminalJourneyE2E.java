package pt.ua.tqs.tuttibox.e2e;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ua.tqs.tuttibox.domain.Size;
import pt.ua.tqs.tuttibox.service.ReservationService;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Journey on the locker terminal: courier drop-off, then recipient pickup with the code received. */
class TerminalJourneyE2E extends AbstractE2E {

    @Autowired
    ReservationService reservations;

    @Test
    void courierDropsOffAndRecipientCollects() {
        Long station = data.station("E2E Station", 40.64, -8.65, Size.M);
        String tracking = reservations.register(station, Size.M, "ana@example.com").trackingCode();

        page.navigate(url("/terminal/" + station));
        page.getByLabel("Tracking code").fill(tracking);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open compartment for drop-off")).click();
        assertThat(page.getByTestId("terminal-message")).containsText("Parcel deposited in compartment M1");

        String code = notifier.pickupCodeFor(tracking).orElseThrow();
        page.getByLabel("Pickup code").fill(code);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Collect parcel")).click();
        assertThat(page.getByTestId("terminal-message")).containsText("Please collect your parcel");

        page.navigate(url("/track?code=" + tracking));
        assertThat(page.getByTestId("tracking-state")).hasText("PICKED_UP");
    }

    @Test
    void wrongPickupCodeIsRejected() {
        Long station = data.station("E2E Station", 40.64, -8.65, Size.M);

        page.navigate(url("/terminal/" + station));
        page.getByLabel("Pickup code").fill("000000");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Collect parcel")).click();

        assertThat(page.getByTestId("terminal-message")).containsText("Invalid pickup code");
    }
}
