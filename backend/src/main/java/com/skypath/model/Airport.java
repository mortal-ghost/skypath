package com.skypath.model;

import java.time.ZoneId;

/**
 * Represents an airport with its IATA code, location, and timezone.
 */
public record Airport(
        String code,
        String name,
        String city,
        String country,
        ZoneId timezone) {
    /**
     * Check if this airport is in the same country as another.
     */
    public boolean isSameCountry(Airport other) {
        return this.country.equals(other.country);
    }
}
