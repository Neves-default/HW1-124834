package pt.ua.tqs.tuttibox.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.ua.tqs.tuttibox.service.StationService;
import pt.ua.tqs.tuttibox.service.StationView;

import java.util.List;

/** UC1 (API): nearest stations to an address. */
@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stations;

    public StationController(StationService stations) {
        this.stations = stations;
    }

    @GetMapping
    public List<StationView> all() {
        return stations.all();
    }

    @GetMapping("/nearest")
    public List<StationView> nearest(@RequestParam String address) {
        return stations.nearest(address);
    }
}
