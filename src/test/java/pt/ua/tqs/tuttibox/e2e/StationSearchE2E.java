package pt.ua.tqs.tuttibox.e2e;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;
import pt.ua.tqs.tuttibox.domain.Size;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Journey on the web pages: the recipient finds the nearest stations (UC1). */
class StationSearchE2E extends AbstractE2E {

    @Test
    void showsTheFiveNearestStationsWithFreeCompartments() {
        for (int i = 1; i <= 6; i++) {
            data.station("Station " + i, 40.64 + i * 0.01, -8.65, Size.S, Size.M);
        }
        geocoder.knows("Rua de Aveiro 10", 40.64, -8.65);

        page.navigate(url("/search"));
        page.getByLabel("Address").fill("Rua de Aveiro 10");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Search")).click();

        assertThat(page.getByTestId("station-row")).hasCount(5);
        assertThat(page.getByTestId("station-row").first()).containsText("Station 1");
    }

    @Test
    void unknownAddressShowsAMessage() {
        page.navigate(url("/search"));
        page.getByLabel("Address").fill("Rua que não existe");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Search")).click();

        assertThat(page.getByTestId("search-error")).hasText("Address not found.");
    }
}
