package com.skypath.service;

import com.skypath.model.Itinerary;

import java.time.LocalDate;
import java.util.List;

/**
 * Core flight search service interface.
 */
public interface SearchService {

    /**
     * Search for valid itineraries from origin to destination on a given date
     * using the default configured maximum number of stops.
     * Returns results sorted by total travel duration.
     *
     * @param origin      IATA code
     * @param destination IATA code
     * @param date        departure date
     * @return sorted list of valid itineraries
     */
    List<Itinerary> search(String origin, String destination, LocalDate date);

    /**
     * Search for valid itineraries from origin to destination on a given date
     * allowing up to the specified number of stops.
     * Returns results sorted by total travel duration.
     *
     * @param origin      IATA code
     * @param destination IATA code
     * @param date        departure date
     * @param maxStops    maximum number of intermediate stops allowed
     * @return sorted list of valid itineraries
     */
    List<Itinerary> search(String origin, String destination, LocalDate date, int maxStops);
}
