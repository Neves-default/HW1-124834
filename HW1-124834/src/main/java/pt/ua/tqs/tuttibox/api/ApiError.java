package pt.ua.tqs.tuttibox.api;

/** Uniform error body for every API error. */
public record ApiError(int status, String error, String message) {
}
