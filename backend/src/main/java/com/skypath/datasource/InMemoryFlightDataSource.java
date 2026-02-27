package com.skypath.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skypath.model.Airport;
import com.skypath.model.Flight;
import com.skypath.service.TimeZoneService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory implementation of FlightDataSource.
 * Loads flights.json on startup and indexes data for efficient querying.
 *
 * This is the initial implementation. Future implementations (e.g.,
 * JpaFlightDataSource,
 * RestApiFlightDataSource) can replace this by implementing the same interface.
 */
@Component
public class InMemoryFlightDataSource implements FlightDataSource {

    private static final Logger log = LoggerFactory.getLogger(InMemoryFlightDataSource.class);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Value("${skypath.data.file}")
    private Resource dataFile;

    private final TimeZoneService timeZoneService;

    // Indexed data structures
    private final Map<String, Airport> airportMap = new LinkedHashMap<>();
    private final Map<String, List<Flight>> flightsByOrigin = new HashMap<>();
    private final Map<String, List<Flight>> flightsByRoute = new HashMap<>(); // "origin:destination"

    public InMemoryFlightDataSource(TimeZoneService timeZoneService) {
        this.timeZoneService = timeZoneService;
    }

    @PostConstruct
    public void loadData() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        try (InputStream is = dataFile.getInputStream()) {
            JsonNode root = mapper.readTree(is);

            loadAirports(root.get("airports"));
            loadFlights(root.get("flights"));
        }

        log.info("Loaded {} airports and {} flights ({} routes)",
                airportMap.size(),
                flightsByOrigin.values().stream().mapToInt(List::size).sum(),
                flightsByRoute.size());
    }

    private void loadAirports(JsonNode airportsNode) {
        for (JsonNode node : airportsNode) {
            String code = node.get("code").asText();
            Airport airport = new Airport(
                    code,
                    node.get("name").asText(),
                    node.get("city").asText(),
                    node.get("country").asText(),
                    ZoneId.of(node.get("timezone").asText()));
            airportMap.put(code, airport);
        }
    }

    private void loadFlights(JsonNode flightsNode) {
        int skipped = 0;

        for (JsonNode node : flightsNode) {
            String origin = node.get("origin").asText();
            String destination = node.get("destination").asText();

            // Validate that airports exist (handles the SP995 "JKF" typo)
            if (!airportMap.containsKey(origin) || !airportMap.containsKey(destination)) {
                log.warn("Skipping flight {} with unknown airport(s): {} → {}",
                        node.get("flightNumber").asText(), origin, destination);
                skipped++;
                continue;
            }

            // Parse price robustly (handles string and numeric values)
            double price = parsePrice(node.get("price"));

            Flight flight = new Flight(
                    node.get("flightNumber").asText(),
                    node.get("airline").asText(),
                    origin,
                    destination,
                    LocalDateTime.parse(node.get("departureTime").asText(), DATE_TIME_FORMAT),
                    LocalDateTime.parse(node.get("arrivalTime").asText(), DATE_TIME_FORMAT),
                    price,
                    node.get("aircraft").asText());

            // Compute UTC times for timezone-correct calculations
            timeZoneService.computeUtcTimes(
                    flight,
                    airportMap.get(origin),
                    airportMap.get(destination));

            // Index by origin
            flightsByOrigin.computeIfAbsent(origin, k -> new ArrayList<>()).add(flight);

            // Index by route
            String routeKey = routeKey(origin, destination);
            flightsByRoute.computeIfAbsent(routeKey, k -> new ArrayList<>()).add(flight);
        }

        if (skipped > 0) {
            log.warn("Skipped {} flights due to unknown airport codes", skipped);
        }
    }

    private double parsePrice(JsonNode priceNode) {
        if (priceNode.isNumber()) {
            return priceNode.asDouble();
        }
        // Handle string prices like "289.00" or "99"
        return Double.parseDouble(priceNode.asText());
    }

    private String routeKey(String origin, String destination) {
        return origin + ":" + destination;
    }

    // ======= FlightDataSource interface implementation =======

    @Override
    public List<Flight> getFlightsFrom(String airportCode, LocalDate date) {
        return getFlightsFrom(airportCode, date, date);
    }

    @Override
    public List<Flight> getFlightsFrom(String airportCode, LocalDate fromDate, LocalDate toDate) {
        List<Flight> flights = flightsByOrigin.getOrDefault(airportCode, Collections.emptyList());
        return filterByDateRange(flights, fromDate, toDate);
    }

    @Override
    public List<Flight> getDirectFlights(String origin, String destination,
            LocalDate fromDate, LocalDate toDate) {
        String key = routeKey(origin, destination);
        List<Flight> flights = flightsByRoute.getOrDefault(key, Collections.emptyList());
        return filterByDateRange(flights, fromDate, toDate);
    }

    @Override
    public List<Flight> getConnectingFlights(String airportCode,
            ZonedDateTime earliestDepartureUtc, ZonedDateTime latestDepartureUtc) {
        List<Flight> flights = flightsByOrigin.getOrDefault(airportCode, Collections.emptyList());
        return flights.stream()
                .filter(f -> {
                    ZonedDateTime depUtc = f.getDepartureUtc();
                    return depUtc != null
                            && !depUtc.isBefore(earliestDepartureUtc)
                            && !depUtc.isAfter(latestDepartureUtc);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Airport> getAirport(String code) {
        return Optional.ofNullable(airportMap.get(code));
    }

    @Override
    public List<Airport> getAllAirports() {
        return List.copyOf(airportMap.values());
    }

    @Override
    public boolean airportExists(String code) {
        return airportMap.containsKey(code);
    }

    /**
     * Filter flights by local departure date range (inclusive on both ends).
     */
    private List<Flight> filterByDateRange(List<Flight> flights, LocalDate fromDate, LocalDate toDate) {
        return flights.stream()
                .filter(f -> {
                    LocalDate depDate = f.getDepartureTime().toLocalDate();
                    return !depDate.isBefore(fromDate) && !depDate.isAfter(toDate);
                })
                .collect(Collectors.toList());
    }
}
