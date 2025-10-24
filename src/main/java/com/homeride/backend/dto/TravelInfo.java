package com.homeride.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelInfo {
    private int durationInMinutes;
    private double distanceInKm;
    private String polyline;
    private String summary;
    private List<Double> segmentDistances = new ArrayList<>(); // NEW: Add this field

    // Constructor without segmentDistances for backwards compatibility
    public TravelInfo(int durationInMinutes, double distanceInKm, String polyline, String summary) {
        this.durationInMinutes = durationInMinutes;
        this.distanceInKm = distanceInKm;
        this.polyline = polyline;
        this.summary = summary;
        this.segmentDistances = new ArrayList<>();
    }
}