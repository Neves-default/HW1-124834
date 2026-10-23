package pt.ua.tqs.tuttibox.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.ua.tqs.tuttibox.service.CompartmentService;
import pt.ua.tqs.tuttibox.service.CompartmentView;

import java.util.List;

/** UC6 (API): compartments of a station, out of service / reactivation. */
@RestController
@RequestMapping("/api")
public class CompartmentController {

    private final CompartmentService compartments;

    public CompartmentController(CompartmentService compartments) {
        this.compartments = compartments;
    }

    @GetMapping("/stations/{stationId}/compartments")
    public List<CompartmentView> list(@PathVariable Long stationId) {
        return compartments.list(stationId);
    }

    @PostMapping("/compartments/{compartmentId}/out-of-service")
    public CompartmentView outOfService(@PathVariable Long compartmentId) {
        return compartments.putOutOfService(compartmentId);
    }

    @PostMapping("/compartments/{compartmentId}/reactivation")
    public CompartmentView reactivate(@PathVariable Long compartmentId) {
        return compartments.reactivate(compartmentId);
    }
}
