package com.skypath.datasource;

import com.skypath.model.Airport;
import com.skypath.model.Flight;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Abstract data source for flight and airport data.
 * 
 * This interface decouples all business logic from the data origin.
 * Implementations can back to in-memory data, a relational database,
 * an external API, a cache layer, or any combination.
 */
public interface FlightDataSource {

    /**
     * Get all flights departing from an airport within a date range.
     * The date range is based on the flight's local departure date.
     *
     * @param airportCode IATA airport code (e.g., "JFK")
     * @param fromDate    start date (inclusive)
     * @param toDate      end date (inclusive)
     * @return flights departing from the airport within the date range
     */
    List<Flight> getFlightsFrom(String airportCode, LocalDate fromDate, LocalDate toDate);

    /**
     * Get direct flights between two specific airports within a date range.
     * This is an optimization — implementations may use indexed queries.
     *
     * @param origin      origin IATA code
     * @param destination destination IATA code
     * @param fromDate    start date (inclusive)
     * @param toDate      end date (inclusive)
     * @return direct flights from origin to destination within the date range
     */
    List<Flight> getDirectFlights(String origin, String destination,
            LocalDate fromDate, LocalDate toDate);

    /**
     * Look up an airport by its IATA code.
     */
    Optional<Airport> getAirport(String code);

    /**
     * Get all known airports.
     */
    List<Airport> getAllAirports();

    /**
     * Check if an airport with the given code exists.
     */
    boolean airportExists(String code);
}
