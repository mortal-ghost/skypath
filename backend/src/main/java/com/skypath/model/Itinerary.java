package com.skypath.model;

import java.util.List;

/**
 * Represents a complete flight itinerary from origin to destination.
 * Contains one or more flight segments, layover information, and aggregate
 * metrics.
 */
public record Itinerary(
        List<FlightSegment> segments,
        List<Layover> layovers,
        int stops,
        long totalDurationMinutes,
        double totalPrice) {
    /**
     * A single flight segment within an itinerary.
     */
    public record FlightSegment(
            String flightNumber,
            String airline,
            String originCode,
            String originName,
            String originCity,
            String destinationCode,
            String destinationName,
            String destinationCity,
            String departureTime,
            String arrivalTime,
            long durationMinutes,
            String aircraft) {
    }

    /**
     * Layover information at a connection point.
     */
    public record Layover(
            String airportCode,
            String airportName,
            String airportCity,
            long durationMinutes,
            String type // "domestic" or "international"
    ) {
    }
}
