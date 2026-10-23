package pt.ua.tqs.tuttibox.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import pt.ua.tqs.tuttibox.domain.Size;
import pt.ua.tqs.tuttibox.service.ReservationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** REST API: resources, status codes and the uniform error body {status, error, message}. */
class ApiIT extends AbstractIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    ReservationService reservations;

    private MockMvc mvc;

    @BeforeEach
    void setUpMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private static String registerBody(Object stationId, String size, String contact) {
        return """
                {"stationId": %s, "size": "%s", "recipientContact": "%s"}
                """.formatted(stationId, size, contact);
    }

    @Test
    void registerReturns201WithATrackingCode() throws Exception {
        Long st = data.station("API station", 40.64, -8.65, Size.M);

        mvc.perform(post("/api/parcels").contentType(MediaType.APPLICATION_JSON).content(registerBody(st, "M", "ana@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingCode").value(matchesPattern("TBX-[A-Z0-9]{8}")))
                .andExpect(jsonPath("$.state").value("CREATED"));
    }

    @Test
    void invalidRequestsReturn400WithTheErrorBody() throws Exception {
        Long st = data.station("API station", 40.64, -8.65, Size.M);

        mvc.perform(post("/api/parcels").contentType(MediaType.APPLICATION_JSON).content(registerBody(st, "XL", "ana@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

        mvc.perform(post("/api/parcels").contentType(MediaType.APPLICATION_JSON).content(registerBody(st, "M", "")))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/parcels").contentType(MediaType.APPLICATION_JSON).content(registerBody(999999, "M", "a@b.pt")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNKNOWN_STATION"));
    }

    @Test
    void fullStationReturns409() throws Exception {
        Long st = data.station("API station", 40.64, -8.65, Size.S);
        reservations.register(st, Size.S, "a@example.com");

        mvc.perform(post("/api/parcels").contentType(MediaType.APPLICATION_JSON).content(registerBody(st, "S", "b@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("NO_FREE_COMPARTMENT"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void invalidTransitionReturns409() throws Exception {
        Long st = data.station("API station", 40.64, -8.65, Size.M);
        String tracking = reservations.register(st, Size.M, "a@example.com").trackingCode();
        reservations.dropOff(st, tracking);

        mvc.perform(post("/api/parcels/{code}/cancel", tracking))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_TRANSITION"));
    }

    @Test
    void trackingShowsHistoryButNeverThePickupCode() throws Exception {
        Long st = data.station("API station", 40.64, -8.65, Size.M);
        String tracking = reservations.register(st, Size.M, "a@example.com").trackingCode();
        reservations.dropOff(st, tracking);
        String code = notifier.pickupCodeFor(tracking).orElseThrow();

        String body = mvc.perform(get("/api/parcels/{code}", tracking))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("READY_FOR_PICKUP"))
                .andExpect(jsonPath("$.history", hasSize(2)))
                .andExpect(jsonPath("$.history[1].actor").value("COURIER"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(code).doesNotContainIgnoringCase("pickupCode");
    }

    @Test
    void unknownTrackingCodeReturns404() throws Exception {
        mvc.perform(get("/api/parcels/{code}", "TBX-NOPE0000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PARCEL_NOT_FOUND"));
    }

    @Test
    void nearestStationsAddressNotFoundAndProviderDown() throws Exception {
        data.station("Near", 40.6406, -8.6539, Size.M);
        geocoder.knows("Rua de Aveiro 10", 40.6405, -8.6538);

        mvc.perform(get("/api/stations/nearest").param("address", "Rua de Aveiro 10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Near"))
                .andExpect(jsonPath("$[0].freeM").value(1));

        mvc.perform(get("/api/stations/nearest").param("address", "Rua que não existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ADDRESS_NOT_FOUND"));

        geocoder.setUnavailable(true);
        mvc.perform(get("/api/stations/nearest").param("address", "Rua de Aveiro 10"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("GEOCODING_UNAVAILABLE"));
    }

    @Test
    void terminalWrongCodesThenBlocked() throws Exception {
        Long st = data.station("API station", 40.64, -8.65, Size.M);
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/terminal/{id}/pickup", st).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"pickupCode\": \"000000\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_CODE"));
        }
        mvc.perform(post("/api/terminal/{id}/pickup", st).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupCode\": \"000000\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("TERMINAL_BLOCKED"));
    }

    @Test
    void occupiedCompartmentCannotBePutOutOfService() throws Exception {
        Long st = data.station("API station", 40.64, -8.65, Size.M);
        String tracking = reservations.register(st, Size.M, "a@example.com").trackingCode();
        Long compartment = data.parcelCompartmentId(tracking);

        mvc.perform(post("/api/compartments/{id}/out-of-service", compartment))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("COMPARTMENT_NOT_EMPTY"));
    }
}
