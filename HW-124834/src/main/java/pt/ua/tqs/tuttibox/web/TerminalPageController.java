package pt.ua.tqs.tuttibox.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pt.ua.tqs.tuttibox.exception.TuttiboxException;
import pt.ua.tqs.tuttibox.service.ReservationService;
import pt.ua.tqs.tuttibox.service.StationService;
import pt.ua.tqs.tuttibox.service.TerminalResult;

/** The locker terminal (touchscreen simulation): courier drop-off (UC3) and recipient pickup (UC4). */
@Controller
@RequestMapping("/terminal/{stationId}")
public class TerminalPageController {

    private final StationService stations;
    private final ReservationService reservations;

    public TerminalPageController(StationService stations, ReservationService reservations) {
        this.stations = stations;
        this.reservations = reservations;
    }

    @GetMapping
    public String page(@PathVariable Long stationId, Model model) {
        model.addAttribute("station", stations.get(stationId));
        return "terminal";
    }

    @PostMapping("/drop-off")
    public String dropOff(@PathVariable Long stationId, @RequestParam String trackingCode, Model model) {
        try {
            show(model, reservations.dropOff(stationId, trackingCode.trim()));
        } catch (TuttiboxException e) {
            showError(model, "PARCEL_NOT_FOUND".equals(e.getCode())
                    ? "Unknown tracking code for this station." : e.getMessage());
        }
        return page(stationId, model);
    }

    @PostMapping("/pickup")
    public String pickup(@PathVariable Long stationId, @RequestParam String pickupCode, Model model) {
        try {
            show(model, reservations.pickup(stationId, pickupCode.trim()));
        } catch (TuttiboxException e) {
            showError(model, e.getMessage());
        }
        return page(stationId, model);
    }

    private static void show(Model model, TerminalResult result) {
        model.addAttribute("ok", result.isOk());
        model.addAttribute("message", result.message());
    }

    private static void showError(Model model, String message) {
        model.addAttribute("ok", false);
        model.addAttribute("message", message);
    }
}
