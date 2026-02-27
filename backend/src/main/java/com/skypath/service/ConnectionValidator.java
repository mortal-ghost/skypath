package com.skypath.service;

import com.skypath.datasource.FlightDataSource;
import com.skypath.model.Airport;
import com.skypath.model.Flight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Validates whether two consecutive flights form a valid connection.
 * Encapsulates all connection rules: same airport, temporal ordering,
 * minimum/maximum layover durations, and domestic vs. international
 * classification.
 */
@Service
public class ConnectionValidator {

    private static final Logger log = LoggerFactory.getLogger(ConnectionValidator.class);

    private static final long MIN_DOMESTIC_LAYOVER_MINUTES = 45;
    private static final long MIN_INTERNATIONAL_LAYOVER_MINUTES = 90;
    private static final long MAX_LAYOVER_MINUTES = 360; // 6 hours

    private final FlightDataSource dataSource;
    private final TimeZoneService timeZoneService;

    public ConnectionValidator(FlightDataSource dataSource, TimeZoneService timeZoneService) {
        this.dataSource = dataSource;
        this.timeZoneService = timeZoneService;
    }

    /**
     * Check if two consecutive flights form a valid connection.
     *
     * @param arriving  the first flight (arriving at the connection point)
     * @param departing the second flight (departing from the connection point)
     * @return true if the connection is valid
     */
    public boolean isValidConnection(Flight arriving, Flight departing) {
        // Rule 1: Must be the same airport (no airport changes, e.g. JFK→LGA)
        if (!arriving.getDestination().equals(departing.getOrigin())) {
            return false;
        }

        // Rule 2: Departing flight must leave after arriving flight lands
        long layoverMinutes = timeZoneService.calculateLayoverMinutes(arriving, departing);
        if (layoverMinutes < 0) {
            return false;
        }

        // Rule 3: Maximum layover
        if (layoverMinutes > MAX_LAYOVER_MINUTES) {
            return false;
        }

        // Rule 4: Minimum layover depends on domestic vs. international
        long minLayover = isDomesticConnection(arriving, departing)
                ? MIN_DOMESTIC_LAYOVER_MINUTES
                : MIN_INTERNATIONAL_LAYOVER_MINUTES;

        return layoverMinutes >= minLayover;
    }

    /**
     * Determine if a connection is domestic (both flights within the same country).
     */
    public boolean isDomesticConnection(Flight arriving, Flight departing) {
        Airport arrOrigin = dataSource.getAirport(arriving.getOrigin()).orElse(null);
        Airport arrDest = dataSource.getAirport(arriving.getDestination()).orElse(null);
        Airport depDest = dataSource.getAirport(departing.getDestination()).orElse(null);

        if (arrOrigin == null || arrDest == null || depDest == null) {
            // If we can't determine country, treat as international (stricter)
            return false;
        }

        // Domestic if ALL airports involved are in the same country
        return arrOrigin.isSameCountry(arrDest) && arrDest.isSameCountry(depDest);
    }

    /**
     * Get the layover duration in minutes between two flights.
     */
    public long getLayoverMinutes(Flight arriving, Flight departing) {
        return timeZoneService.calculateLayoverMinutes(arriving, departing);
    }
}
