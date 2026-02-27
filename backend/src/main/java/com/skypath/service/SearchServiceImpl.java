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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core flight search service implementing a recursive depth-limited DFS.
 *
 * <p>
 * Algorithm overview:
 * <ol>
 * <li>Fetch all flights departing from the origin on the search date.</li>
 * <li>Separate direct flights to the destination — add them to results
 * immediately.</li>
 * <li>For remaining flights (to intermediate airports), run DFS.</li>
 * <li>At each DFS node, query connecting flights using a precise UTC time
 * window
 * (min/max layover for domestic/international), reducing unnecessary data
 * fetch.</li>
 * </ol>
 */
@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    static final long MIN_DOMESTIC_LAYOVER_MINUTES = 45;
    static final long MIN_INTERNATIONAL_LAYOVER_MINUTES = 90;
    static final long MAX_LAYOVER_MINUTES = 360; // 6 hours

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

        // Step 1: Fetch all flights from origin on the search date (single data call)
        List<Flight> allOriginFlights = dataSource.getFlightsFrom(origin, date);

        // Step 2: Extract direct flights to destination
        List<Flight> directFlights = allOriginFlights.stream()
                .filter(f -> f.getDestination().equals(destination))
                .toList();

        for (Flight direct : directFlights) {
            results.add(buildItinerary(List.of(direct)));
        }

        // Step 3: If stops allowed, run DFS on remaining flights (to intermediate
        // airports)
        if (maxStops > 0) {
            List<Flight> intermediateFlights = allOriginFlights.stream()
                    .filter(f -> !f.getDestination().equals(destination))
                    .toList();

            // Group by destination to avoid processing the same intermediate twice
            Map<String, List<Flight>> byDestination = intermediateFlights.stream()
                    .collect(Collectors.groupingBy(Flight::getDestination));

            Set<String> visited = new HashSet<>();
            visited.add(origin);

            for (Map.Entry<String, List<Flight>> entry : byDestination.entrySet()) {
                String nextAirport = entry.getKey();

                // Domestic constraint: skip foreign intermediates
                if (domesticCountry != null && !isInCountry(nextAirport, domesticCountry)) {
                    continue;
                }

                visited.add(nextAirport);

                for (Flight flight : entry.getValue()) {
                    List<Flight> path = new ArrayList<>();
                    path.add(flight);
                    explore(destination, visited, path, flight,
                            maxStops - 1, isDomestic, domesticCountry, results);
                }

                visited.remove(nextAirport);
            }
        }

        // Sort by total travel duration (shortest first)
        results.sort(Comparator.comparingLong(Itinerary::totalDurationMinutes));

        log.info("Found {} itineraries for {} → {} on {}", results.size(), origin, destination, date);
        return results;
    }

    /**
     * Recursive DFS that fetches connecting flights using a precise UTC time
     * window.
     *
     * @param destination     target destination code
     * @param visited         set of already-visited airport codes (prevents cycles)
     * @param path            current path of flights
     * @param lastFlight      the last flight in the current path
     * @param remainingStops  how many more intermediate stops are allowed
     * @param isDomestic      whether the overall search is domestic
     * @param domesticCountry if non-null, skip foreign intermediate airports
     * @param results         accumulator for valid itineraries
     */
    private void explore(String destination, Set<String> visited,
            List<Flight> path, Flight lastFlight,
            int remainingStops, boolean isDomestic,
            String domesticCountry, List<Itinerary> results) {

        String currentAirport = lastFlight.getDestination();

        // Compute the valid connection time window based on domestic/international
        long minLayover = isDomestic ? MIN_DOMESTIC_LAYOVER_MINUTES : MIN_INTERNATIONAL_LAYOVER_MINUTES;
        ZonedDateTime arrivalUtc = lastFlight.getArrivalUtc();
        ZonedDateTime earliestDeparture = arrivalUtc.plusMinutes(minLayover);
        ZonedDateTime latestDeparture = arrivalUtc.plusMinutes(MAX_LAYOVER_MINUTES);

        // Fetch only connecting flights within the valid layover window
        List<Flight> connectingFlights = dataSource.getConnectingFlights(
                currentAirport, earliestDeparture, latestDeparture);

        // Step 1: Try direct connections to the destination
        for (Flight flight : connectingFlights) {
            if (!flight.getDestination().equals(destination)) {
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
        Map<String, List<Flight>> intermediates = connectingFlights.stream()
                .filter(f -> !f.getDestination().equals(destination))
                .collect(Collectors.groupingBy(Flight::getDestination));

        for (Map.Entry<String, List<Flight>> entry : intermediates.entrySet()) {
            String nextAirport = entry.getKey();

            if (visited.contains(nextAirport)) {
                continue;
            }

            // Domestic constraint: skip foreign intermediates
            if (domesticCountry != null && !isInCountry(nextAirport, domesticCountry)) {
                continue;
            }

            visited.add(nextAirport);

            for (Flight flight : entry.getValue()) {
                List<Flight> newPath = new ArrayList<>(path);
                newPath.add(flight);
                explore(destination, visited, newPath, flight,
                        remainingStops - 1, isDomestic, domesticCountry, results);
            }

            visited.remove(nextAirport);
        }
    }

    /**
     * Check if an airport is in the given country.
     */
    private boolean isInCountry(String airportCode, String country) {
        return dataSource.getAirport(airportCode)
                .map(a -> country.equals(a.country()))
                .orElse(false);
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
