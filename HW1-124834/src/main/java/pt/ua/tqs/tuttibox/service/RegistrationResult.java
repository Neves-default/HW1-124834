package pt.ua.tqs.tuttibox.service;

import pt.ua.tqs.tuttibox.domain.ParcelState;
import pt.ua.tqs.tuttibox.domain.Size;

public record RegistrationResult(String trackingCode, ParcelState state, Long stationId,
                                 String compartmentCode, Size compartmentSize) {
}
