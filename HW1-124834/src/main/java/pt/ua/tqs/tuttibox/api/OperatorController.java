package pt.ua.tqs.tuttibox.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.ua.tqs.tuttibox.domain.ParcelState;
import pt.ua.tqs.tuttibox.service.ParcelSummary;
import pt.ua.tqs.tuttibox.service.ReservationService;
import pt.ua.tqs.tuttibox.service.TrackingView;

import java.util.List;

/** UC6 (API): list parcels by state and register the removal of expired parcels. */
@RestController
@RequestMapping("/api/operator")
public class OperatorController {

    private final ReservationService reservations;

    public OperatorController(ReservationService reservations) {
        this.reservations = reservations;
    }

    @GetMapping("/stations/{stationId}/parcels")
    public List<ParcelSummary> parcels(@PathVariable Long stationId, @RequestParam(required = false) ParcelState state) {
        return reservations.listParcels(stationId, state);
    }

    @PostMapping("/parcels/{trackingCode}/removal")
    public TrackingView registerRemoval(@PathVariable String trackingCode) {
        return reservations.registerRemoval(trackingCode);
    }
}
