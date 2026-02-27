package com.skypath.service;

import com.skypath.datasource.FlightDataSource;
import com.skypath.model.Airport;
import com.skypath.model.Flight;
import com.skypath.model.Itinerary;
import com.skypath.model.Itinerary.FlightSegment;
import com.skypath.model.Itinerary.Layover;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core flight search service implementing a recursive depth-limited DFS.
 * 
 * The algorithm generalizes to N hops via the maxStops parameter.
 * Flights are fetched lazily per-airport as the recursion descends, so the
 * search never requires the full dataset in memory at once.
 */
@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Value("${skypath.search.max-stops:2}")
    private int defaultMaxStops;

    private final FlightDataSource dataSource;
    private final ConnectionValidator connectionValidator;
    private final TimeZoneService timeZoneService;

    public SearchServiceImpl(FlightDataSource dataSource,
            ConnectionValidator connectionValidator,
            TimeZoneService timeZoneService) {
        this.dataSource = dataSource;
        this.connectionValidator = connectionValidator;
        this.timeZoneService = timeZoneService;
    }

    @Override
    public List<Itinerary> search(String origin, String destination, LocalDate date) {
        return search(origin, destination, date, defaultMaxStops);
    }

    @Override
    public List<Itinerary> search(String origin, String destination, LocalDate date, int maxStops) {
        return search(origin, destination, date, maxStops, false);
    }

    @Override
    public List<Itinerary> search(String origin, String destination, LocalDate date, int maxStops, boolean isDomestic) {
        log.info("Searching {} → {} on {} (max stops: {}, domestic: {})",
                origin, destination, date, maxStops, isDomestic);

        // Resolve the country constraint for domestic searches
        String domesticCountry = null;
        if (isDomestic) {
            domesticCountry = dataSource.getAirport(origin)
                    .map(Airport::country)
                    .orElse(null);
        }

        List<Itinerary> results = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(origin);

        // First-leg flights must depart on the search date.
        // Connecting flights may depart up to the next day (overnight connections).
        explore(origin, destination, date, date, visited,
                new ArrayList<>(), null, maxStops, domesticCountry, results);

        // Sort by total travel duration (shortest first)
        results.sort(Comparator.comparingLong(Itinerary::totalDurationMinutes));

        log.info("Found {} itineraries for {} → {} on {}", results.size(), origin, destination, date);
        return results;
    }

    /**
     * Recursive DFS exploration.
     * 
     * At each node:
     * 1. Try direct flights to destination (always, regardless of remaining stops)
     * 2. If stops remain, recurse through intermediates
     *
     * @param current         current airport code
     * @param destination     target destination code
     * @param fromDate        date range start
     * @param toDate          date range end
     * @param visited         set of already-visited airport codes (prevents cycles)
     * @param path            current path of flights
     * @param lastFlight      the last flight in the current path (null at root)
     * @param remainingStops  how many more intermediate stops are allowed
     * @param domesticCountry if non-null, only intermediate airports in this
     *                        country are considered
     * @param results         accumulator for valid itineraries
     */
    private void explore(String current, String destination,
            LocalDate fromDate, LocalDate toDate,
            Set<String> visited, List<Flight> path,
            Flight lastFlight, int remainingStops,
            String domesticCountry, List<Itinerary> results) {

        // Step 1: Try direct flights from current → destination
        List<Flight> directFlights = dataSource.getDirectFlights(current, destination, fromDate, toDate);
        for (Flight flight : directFlights) {
            if (lastFlight != null && !connectionValidator.isValidConnection(lastFlight, flight)) {
                continue;
            }
            List<Flight> completePath = new ArrayList<>(path);
            completePath.add(flight);
            results.add(buildItinerary(completePath));
        }

        // Step 2: Base case — no more stops allowed
        if (remainingStops == 0) {
            return;
        }

        // Step 3: Recurse through intermediates
        List<Flight> departingFlights = dataSource.getFlightsFrom(current, fromDate, toDate);

        // Group by destination to query each intermediate airport only once
        Map<String, List<Flight>> intermediates = departingFlights.stream()
                .collect(Collectors.groupingBy(Flight::getDestination));

        for (Map.Entry<String, List<Flight>> entry : intermediates.entrySet()) {
            String nextAirport = entry.getKey();

            // Skip destinations already handled or visited
            if (nextAirport.equals(destination))
                continue; // already handled in step 1
            if (visited.contains(nextAirport))
                continue; // no circular routes

            // For domestic searches, skip intermediate airports outside the domestic
            // country
            if (domesticCountry != null) {
                Airport nextAirportObj = dataSource.getAirport(nextAirport).orElse(null);
                if (nextAirportObj == null || !domesticCountry.equals(nextAirportObj.country())) {
                    continue;
                }
            }

            visited.add(nextAirport);

            for (Flight flight : entry.getValue()) {
                // Validate connection with previous flight
                if (lastFlight != null && !connectionValidator.isValidConnection(lastFlight, flight)) {
                    continue;
                }

                List<Flight> newPath = new ArrayList<>(path);
                newPath.add(flight);

                // Connecting flights can depart on the arrival date or the next day
                LocalDate connectionDate = flight.getArrivalTime().toLocalDate();
                explore(nextAirport, destination, connectionDate, connectionDate.plusDays(1), visited,
                        newPath, flight, remainingStops - 1, domesticCountry, results);
            }

            visited.remove(nextAirport); // backtrack to allow other branches
        }
    }

    /**
     * Build an Itinerary from a completed path of flights.
     */
    private Itinerary buildItinerary(List<Flight> flights) {
        List<FlightSegment> segments = new ArrayList<>();
        List<Layover> layovers = new ArrayList<>();
        double totalPrice = 0;

        for (int i = 0; i < flights.size(); i++) {
            Flight flight = flights.get(i);
            Airport originAirport = dataSource.getAirport(flight.getOrigin()).orElseThrow();
            Airport destAirport = dataSource.getAirport(flight.getDestination()).orElseThrow();
            long durationMinutes = timeZoneService.calculateFlightDurationMinutes(flight);

            segments.add(new FlightSegment(
                    flight.getFlightNumber(),
                    flight.getAirline(),
                    flight.getOrigin(),
                    originAirport.name(),
                    originAirport.city(),
                    flight.getDestination(),
                    destAirport.name(),
                    destAirport.city(),
                    flight.getDepartureTime().format(DISPLAY_FORMAT),
                    flight.getArrivalTime().format(DISPLAY_FORMAT),
                    durationMinutes,
                    flight.getAircraft()));

            totalPrice += flight.getPrice();

            // Build layover info for connections (not after the last flight)
            if (i < flights.size() - 1) {
                Flight nextFlight = flights.get(i + 1);
                long layoverMinutes = connectionValidator.getLayoverMinutes(flight, nextFlight);
                Airport connectionAirport = destAirport;
                String layoverType = connectionValidator.isDomesticConnection(flight, nextFlight)
                        ? "domestic"
                        : "international";

                layovers.add(new Layover(
                        connectionAirport.code(),
                        connectionAirport.name(),
                        connectionAirport.city(),
                        layoverMinutes,
                        layoverType));
            }
        }

        // Total duration: from first departure to last arrival (in UTC)
        Flight first = flights.getFirst();
        Flight last = flights.getLast();
        long totalDuration = java.time.Duration.between(
                first.getDepartureUtc(), last.getArrivalUtc()).toMinutes();

        return new Itinerary(
                segments,
                layovers,
                flights.size() - 1, // stops = segments - 1
                totalDuration,
                Math.round(totalPrice * 100.0) / 100.0);
    }
}
