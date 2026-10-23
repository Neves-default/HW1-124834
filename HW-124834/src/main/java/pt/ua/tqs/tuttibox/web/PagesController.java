package pt.ua.tqs.tuttibox.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pt.ua.tqs.tuttibox.exception.TuttiboxException;
import pt.ua.tqs.tuttibox.service.ReservationService;
import pt.ua.tqs.tuttibox.service.StationService;

/** Recipient pages: home, find a locker (UC1), track a parcel (UC5). */
@Controller
public class PagesController {

    private final StationService stations;
    private final ReservationService reservations;

    public PagesController(StationService stations, ReservationService reservations) {
        this.stations = stations;
        this.reservations = reservations;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("stations", stations.all());
        return "index";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String address, Model model) {
        model.addAttribute("address", address);
        if (address != null && !address.isBlank()) {
            try {
                model.addAttribute("results", stations.nearest(address));
            } catch (TuttiboxException e) {
                model.addAttribute("error", "ADDRESS_NOT_FOUND".equals(e.getCode())
                        ? "Address not found." : "Service temporarily unavailable. Please try again later.");
            }
        }
        return "search";
    }

    @GetMapping("/track")
    public String track(@RequestParam(required = false) String code, Model model) {
        model.addAttribute("code", code);
        if (code != null && !code.isBlank()) {
            try {
                model.addAttribute("parcel", reservations.track(code));
            } catch (TuttiboxException e) {
                model.addAttribute("error", "Parcel not found.");
            }
        }
        return "track";
    }
}
