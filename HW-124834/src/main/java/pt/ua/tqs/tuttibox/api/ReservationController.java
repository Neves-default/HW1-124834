package pt.ua.tqs.tuttibox.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.ua.tqs.tuttibox.service.RegistrationResult;
import pt.ua.tqs.tuttibox.service.ReservationService;
import pt.ua.tqs.tuttibox.service.TrackingView;

/** UC2 (API, partner): register and cancel parcels. */
@RestController
@RequestMapping("/api/parcels")
public class ReservationController {

    private final ReservationService reservations;

    public ReservationController(ReservationService reservations) {
        this.reservations = reservations;
    }

    @PostMapping
    public ResponseEntity<RegistrationResult> register(@Valid @RequestBody RegisterParcelRequest request) {
        RegistrationResult result = reservations.register(request.stationId(), request.size(), request.recipientContact());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/{trackingCode}/cancel")
    public TrackingView cancel(@PathVariable String trackingCode) {
        return reservations.cancel(trackingCode);
    }
}
