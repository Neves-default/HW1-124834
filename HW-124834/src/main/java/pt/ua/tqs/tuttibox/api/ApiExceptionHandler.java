package pt.ua.tqs.tuttibox.api;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pt.ua.tqs.tuttibox.exception.TuttiboxException;

@RestControllerAdvice(basePackages = "pt.ua.tqs.tuttibox.api")
public class ApiExceptionHandler {

    @ExceptionHandler(TuttiboxException.class)
    public ResponseEntity<ApiError> business(TuttiboxException e) {
        return ResponseEntity.status(e.getStatus()).body(new ApiError(e.getStatus(), e.getCode(), e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> invalidRequest(Exception e) {
        return ResponseEntity.badRequest().body(new ApiError(400, "INVALID_REQUEST", "Invalid request: " + shortMessage(e)));
    }

    private static String shortMessage(Exception e) {
        String m = e.getMessage();
        if (m == null) {
            return e.getClass().getSimpleName();
        }
        return m.length() > 200 ? m.substring(0, 200) + "..." : m;
    }
}
