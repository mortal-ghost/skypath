package com.skypath.service;

import com.skypath.datasource.FlightDataSource;
import com.skypath.model.Airport;
import com.skypath.model.Itinerary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Orchestrates flight searches with route-type-aware stop limits.
 *
 * <ul>
 * <li><b>Domestic routes</b> (same country): max 1 stop, only domestic
 * connections allowed.</li>
 * <li><b>International routes</b> (different countries): max 2 stops.</li>
 * </ul>
 *
 * This service does not replace {@link SearchService}; it layers business
 * policy on top of the generic recursive search.
 */
@Service
public class FlightSearchOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(FlightSearchOrchestrator.class);

    private static final int DOMESTIC_MAX_STOPS = 1;
    private static final int INTERNATIONAL_MAX_STOPS = 2;

    private final SearchService searchService;
    private final FlightDataSource dataSource;

    public FlightSearchOrchestrator(SearchService searchService, FlightDataSource dataSource) {
        this.searchService = searchService;
        this.dataSource = dataSource;
    }

    /**
     * Search for itineraries, automatically applying the correct stop limit
     * and connection filtering based on whether the route is domestic or
     * international.
     *
     * @param origin      IATA code
     * @param destination IATA code
     * @param date        departure date
     * @return sorted list of valid itineraries
     */
    public List<Itinerary> search(String origin, String destination, LocalDate date) {
        boolean domestic = isDomesticRoute(origin, destination);
        int maxStops = domestic ? DOMESTIC_MAX_STOPS : INTERNATIONAL_MAX_STOPS;

        log.info("Route {} → {} classified as {} (max stops: {})",
                origin, destination, domestic ? "domestic" : "international", maxStops);

        return searchService.search(origin, destination, date, maxStops, domestic);
    }

    /**
     * Determine whether a route is domestic (both endpoints in the same country).
     */
    private boolean isDomesticRoute(String originCode, String destinationCode) {
        Airport origin = dataSource.getAirport(originCode).orElse(null);
        Airport destination = dataSource.getAirport(destinationCode).orElse(null);

        if (origin == null || destination == null) {
            // Unknown airports — treat as international (stricter rules)
            return false;
        }

        return origin.isSameCountry(destination);
    }
}
