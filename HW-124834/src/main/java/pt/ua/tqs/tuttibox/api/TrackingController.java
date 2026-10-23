package pt.ua.tqs.tuttibox.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.ua.tqs.tuttibox.service.ReservationService;
import pt.ua.tqs.tuttibox.service.TrackingView;

/** UC5 (API): current state and timestamped history. */
@RestController
@RequestMapping("/api/parcels")
public class TrackingController {

    private final ReservationService reservations;

    public TrackingController(ReservationService reservations) {
        this.reservations = reservations;
    }

    @GetMapping("/{trackingCode}")
    public TrackingView track(@PathVariable String trackingCode) {
        return reservations.track(trackingCode);
    }
}
