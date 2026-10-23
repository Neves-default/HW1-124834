package pt.ua.tqs.tuttibox.api;

import jakarta.validation.constraints.NotBlank;

public record DropOffRequest(@NotBlank String trackingCode) {
}
