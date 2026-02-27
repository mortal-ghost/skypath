package com.skypath.controller;

import com.skypath.datasource.FlightDataSource;
import com.skypath.dto.SearchResponse;
import com.skypath.exception.InvalidSearchException;
import com.skypath.model.Itinerary;
import com.skypath.service.FlightSearchOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * REST controller for flight search operations.
 */
@RestController
@RequestMapping("/api/flights")
public class FlightSearchController {

    private final FlightSearchOrchestrator searchOrchestrator;
    private final FlightDataSource dataSource;

    public FlightSearchController(FlightSearchOrchestrator searchOrchestrator, FlightDataSource dataSource) {
        this.searchOrchestrator = searchOrchestrator;
        this.dataSource = dataSource;
    }

    /**
     * Search for flight itineraries.
     *
     * @param origin      origin IATA code (e.g., "JFK")
     * @param destination destination IATA code (e.g., "LAX")
     * @param date        departure date in ISO format (e.g., "2024-03-15")
     * @return list of valid itineraries sorted by travel duration
     */
    @GetMapping("/search")
    public ResponseEntity<SearchResponse> searchFlights(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam String date) {

        // Normalize to uppercase
        origin = origin.trim().toUpperCase();
        destination = destination.trim().toUpperCase();

        // Validate inputs
        validateSearchParams(origin, destination, date);

        LocalDate searchDate = LocalDate.parse(date);
        List<Itinerary> itineraries = searchOrchestrator.search(origin, destination, searchDate);

        SearchResponse response = new SearchResponse(
                origin,
                destination,
                date,
                itineraries.size(),
                itineraries);

        return ResponseEntity.ok(response);
    }

    private void validateSearchParams(String origin, String destination, String date) {
        if (origin.isEmpty() || destination.isEmpty() || date.isEmpty()) {
            throw new InvalidSearchException("Origin, destination, and date are required.");
        }

        if (!isValidIataCode(origin)) {
            throw new InvalidSearchException(
                    "Invalid origin airport code: '" + origin + "'. Must be a 3-letter IATA code.");
        }

        if (!isValidIataCode(destination)) {
            throw new InvalidSearchException(
                    "Invalid destination airport code: '" + destination + "'. Must be a 3-letter IATA code.");
        }

        if (!dataSource.airportExists(origin)) {
            throw new InvalidSearchException(
                    "Unknown origin airport: '" + origin + "'. Airport not found in our database.");
        }

        if (!dataSource.airportExists(destination)) {
            throw new InvalidSearchException(
                    "Unknown destination airport: '" + destination + "'. Airport not found in our database.");
        }

        if (origin.equals(destination)) {
            throw new InvalidSearchException(
                    "Origin and destination cannot be the same airport: '" + origin + "'.");
        }

        try {
            LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new InvalidSearchException(
                    "Invalid date format: '" + date + "'. Expected format: YYYY-MM-DD.");
        }
    }

    private boolean isValidIataCode(String code) {
        return code.length() == 3 && code.chars().allMatch(Character::isLetter);
    }
}
