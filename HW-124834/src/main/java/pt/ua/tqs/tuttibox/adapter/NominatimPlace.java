package pt.ua.tqs.tuttibox.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One result of the Nominatim /search endpoint (only the fields we use). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NominatimPlace(String lat, String lon, String display_name) {
}
