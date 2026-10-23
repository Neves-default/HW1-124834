package pt.ua.tqs.tuttibox.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import pt.ua.tqs.tuttibox.exception.TuttiboxException;

/** Friendly error page for the web pages (e.g. unknown station). */
@ControllerAdvice(basePackages = "pt.ua.tqs.tuttibox.web")
public class WebExceptionHandler {

    @ExceptionHandler(TuttiboxException.class)
    public String handle(TuttiboxException e, Model model, HttpServletResponse response) {
        response.setStatus(e.getStatus());
        model.addAttribute("message", e.getMessage());
        return "error-page";
    }
}
