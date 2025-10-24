package com.homeride.backend.dto;

import java.time.LocalDate;

public class SearchRequestDTO {
    private String origin;
    private String destination;
    private LocalDate travelDateTime;
    private Integer passengerCount;

    // Getters and Setters
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public LocalDate getTravelDateTime() { return travelDateTime; }
    public void setTravelDateTime(LocalDate travelDateTime) { this.travelDateTime = travelDateTime; }
    public Integer getPassengerCount() { return passengerCount; }
    public void setPassengerCount(Integer passengerCount) { this.passengerCount = passengerCount; }
}