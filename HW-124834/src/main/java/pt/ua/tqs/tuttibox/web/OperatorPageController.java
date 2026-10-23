package pt.ua.tqs.tuttibox.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.ua.tqs.tuttibox.domain.ParcelState;
import pt.ua.tqs.tuttibox.exception.TuttiboxException;
import pt.ua.tqs.tuttibox.service.CompartmentService;
import pt.ua.tqs.tuttibox.service.ReservationService;
import pt.ua.tqs.tuttibox.service.StationService;

/** Operator dashboard of one station (UC6). */
@Controller
@RequestMapping("/operator/{stationId}")
public class OperatorPageController {

    private final StationService stations;
    private final ReservationService reservations;
    private final CompartmentService compartments;

    public OperatorPageController(StationService stations, ReservationService reservations,
                                  CompartmentService compartments) {
        this.stations = stations;
        this.reservations = reservations;
        this.compartments = compartments;
    }

    @GetMapping
    public String page(@PathVariable Long stationId, @RequestParam(required = false) ParcelState state, Model model) {
        model.addAttribute("station", stations.get(stationId));
        model.addAttribute("states", ParcelState.values());
        model.addAttribute("selectedState", state);
        model.addAttribute("parcels", reservations.listParcels(stationId, state));
        model.addAttribute("compartments", compartments.list(stationId));
        return "operator";
    }

    @PostMapping("/parcels/{trackingCode}/removal")
    public String removal(@PathVariable Long stationId, @PathVariable String trackingCode, RedirectAttributes flash) {
        run(flash, () -> reservations.registerRemoval(trackingCode), "Removal of " + trackingCode + " registered.");
        return "redirect:/operator/" + stationId;
    }

    @PostMapping("/compartments/{compartmentId}/out-of-service")
    public String outOfService(@PathVariable Long stationId, @PathVariable Long compartmentId, RedirectAttributes flash) {
        run(flash, () -> compartments.putOutOfService(compartmentId), "Compartment put out of service.");
        return "redirect:/operator/" + stationId;
    }

    @PostMapping("/compartments/{compartmentId}/reactivation")
    public String reactivate(@PathVariable Long stationId, @PathVariable Long compartmentId, RedirectAttributes flash) {
        run(flash, () -> compartments.reactivate(compartmentId), "Compartment reactivated.");
        return "redirect:/operator/" + stationId;
    }

    private static void run(RedirectAttributes flash, Runnable action, String successMessage) {
        try {
            action.run();
            flash.addFlashAttribute("ok", true);
            flash.addFlashAttribute("message", successMessage);
        } catch (TuttiboxException e) {
            flash.addFlashAttribute("ok", false);
            flash.addFlashAttribute("message", e.getMessage());
        }
    }
}
