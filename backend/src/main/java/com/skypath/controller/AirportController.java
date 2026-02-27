package com.skypath.controller;

import com.skypath.datasource.FlightDataSource;
import com.skypath.model.Airport;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for airport data (used for frontend autocomplete).
 */
@RestController
@RequestMapping("/api/airports")
public class AirportController {

    private final FlightDataSource dataSource;

    public AirportController(FlightDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<List<Airport>> getAllAirports() {
        return ResponseEntity.ok(dataSource.getAllAirports());
    }
}
