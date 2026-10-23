package pt.ua.tqs.tuttibox.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pt.ua.tqs.tuttibox.domain.Size;

public record RegisterParcelRequest(@NotNull Long stationId, @NotNull Size size, @NotBlank String recipientContact) {
}
