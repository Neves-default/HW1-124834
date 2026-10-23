package pt.ua.tqs.tuttibox.api;

import jakarta.validation.constraints.NotBlank;

public record PickupRequest(@NotBlank String pickupCode) {
}
