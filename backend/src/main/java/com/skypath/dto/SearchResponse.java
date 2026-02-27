package com.skypath.dto;

import com.skypath.model.Itinerary;

import java.util.List;

/**
 * API response for flight search results.
 */
public record SearchResponse(
        String origin,
        String destination,
        String date,
        int resultCount,
        List<Itinerary> itineraries) {
}
