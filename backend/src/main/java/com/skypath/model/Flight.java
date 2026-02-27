package com.skypath.model;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * Represents a single flight with its schedule, pricing, and computed UTC
 * times.
 * 
 * Local times (departureTime, arrivalTime) come directly from the dataset.
 * UTC times (departureUtc, arrivalUtc) are computed during data loading using
 * airport timezone info,
 * enabling accurate duration and layover calculations across timezones.
 */
public class Flight {

    private String flightNumber;
    private String airline;
    private String origin;
    private String destination;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private double price;
    private String aircraft;

    // Computed during data loading — not in the JSON
    private ZonedDateTime departureUtc;
    private ZonedDateTime arrivalUtc;

    public Flight() {
    }

    public Flight(String flightNumber, String airline, String origin, String destination,
            LocalDateTime departureTime, LocalDateTime arrivalTime, double price, String aircraft) {
        this.flightNumber = flightNumber;
        this.airline = airline;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.price = price;
        this.aircraft = aircraft;
    }

    // Getters and setters

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getAirline() {
        return airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getAircraft() {
        return aircraft;
    }

    public void setAircraft(String aircraft) {
        this.aircraft = aircraft;
    }

    public ZonedDateTime getDepartureUtc() {
        return departureUtc;
    }

    public void setDepartureUtc(ZonedDateTime departureUtc) {
        this.departureUtc = departureUtc;
    }

    public ZonedDateTime getArrivalUtc() {
        return arrivalUtc;
    }

    public void setArrivalUtc(ZonedDateTime arrivalUtc) {
        this.arrivalUtc = arrivalUtc;
    }

    @Override
    public String toString() {
        return flightNumber + " " + origin + "→" + destination +
                " (" + departureTime + " → " + arrivalTime + ")";
    }
}
