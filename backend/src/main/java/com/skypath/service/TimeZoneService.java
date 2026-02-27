package com.skypath.service;

import com.skypath.model.Airport;
import com.skypath.model.Flight;
import org.springframework.stereotype.Service;

import java.time.*;

/**
 * Handles timezone conversions and duration calculations.
 * All duration calculations use UTC to ensure correctness across timezones
 * and date-line crossings.
 */
@Service
public class TimeZoneService {

    /**
     * Convert a flight's local departure/arrival times to UTC using airport
     * timezone data.
     * This should be called during data loading to precompute UTC times.
     */
    public void computeUtcTimes(Flight flight, Airport originAirport, Airport destinationAirport) {
        ZonedDateTime departureZoned = flight.getDepartureTime()
                .atZone(originAirport.timezone());
        ZonedDateTime arrivalZoned = flight.getArrivalTime()
                .atZone(destinationAirport.timezone());

        flight.setDepartureUtc(departureZoned.withZoneSameInstant(ZoneOffset.UTC));
        flight.setArrivalUtc(arrivalZoned.withZoneSameInstant(ZoneOffset.UTC));
    }

    /**
     * Calculate the flight duration in minutes using UTC times.
     * Handles date-line crossings correctly (e.g., NRT→LAX where
     * arrival local time appears "before" departure local time).
     */
    public long calculateFlightDurationMinutes(Flight flight) {
        return Duration.between(flight.getDepartureUtc(), flight.getArrivalUtc()).toMinutes();
    }

    /**
     * Calculate the layover duration in minutes between an arriving and departing
     * flight.
     * Uses UTC times for accuracy across timezones.
     *
     * @return layover duration in minutes (negative if departing before arriving)
     */
    public long calculateLayoverMinutes(Flight arrivingFlight, Flight departingFlight) {
        return Duration.between(arrivingFlight.getArrivalUtc(), departingFlight.getDepartureUtc()).toMinutes();
    }
}
