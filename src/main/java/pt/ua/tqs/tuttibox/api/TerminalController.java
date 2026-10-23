package pt.ua.tqs.tuttibox.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.ua.tqs.tuttibox.service.ReservationService;
import pt.ua.tqs.tuttibox.service.TerminalResult;

/** UC3/UC4 (API): drop-off and pickup at a station's terminal. */
@RestController
@RequestMapping("/api/terminal/{stationId}")
public class TerminalController {

    private final ReservationService reservations;

    public TerminalController(ReservationService reservations) {
        this.reservations = reservations;
    }

    @PostMapping("/drop-off")
    public ResponseEntity<?> dropOff(@PathVariable Long stationId, @Valid @RequestBody DropOffRequest request) {
        return respond(reservations.dropOff(stationId, request.trackingCode()));
    }

    @PostMapping("/pickup")
    public ResponseEntity<?> pickup(@PathVariable Long stationId, @Valid @RequestBody PickupRequest request) {
        return respond(reservations.pickup(stationId, request.pickupCode()));
    }

    private static ResponseEntity<?> respond(TerminalResult result) {
        HttpStatus status = switch (result.status()) {
            case OK -> HttpStatus.OK;
            case INVALID_CODE -> HttpStatus.BAD_REQUEST;
            case HARDWARE_FAILURE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        if (result.isOk()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(status).body(new ApiError(status.value(), result.status().name(), result.message()));
    }
}
