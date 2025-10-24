package com.homeride.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RideRequestDTO {
    private String originCity;
    private String origin;
    private String destinationCity;
    private String destination;
    // UPDATED: This now uses the StopoverDto
    private List<StopoverDto> stops;
    private LocalDateTime travelDateTime;
    private String vehicleModel;
    private Integer vehicleCapacity;
    private String genderPreference;
    private Double price;
    private String driverNote;
    private List<Double> stopoverPrices;
}