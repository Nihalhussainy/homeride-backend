package com.homeride.backend.controller;

import com.google.maps.model.LatLng;
import com.homeride.backend.service.GoogleMapsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/maps")
public class MapsProxyController {

    private final GoogleMapsService googleMapsService;

    @Autowired
    public MapsProxyController(GoogleMapsService googleMapsService) {
        this.googleMapsService = googleMapsService;
    }

    @GetMapping("/geocode")
    public ResponseEntity<LatLng> geocodeAddress(@RequestParam String address) {
        try {
            LatLng location = googleMapsService.geocodeAddress(address);
            if (location != null) {
                return ResponseEntity.ok(location);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Geocoding error: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // FIXED: Changed from POST to GET with query parameters
    @GetMapping("/reverse-geocode")
    public ResponseEntity<Map<String, String>> reverseGeocode(
            @RequestParam double lat,
            @RequestParam double lng) {
        try {
            LatLng location = new LatLng(lat, lng);
            String address = googleMapsService.reverseGeocode(location);

            if (address != null && !address.equals("Unknown location")) {
                return ResponseEntity.ok(Map.of("address", address));
            }
            return ResponseEntity.ok(Map.of("address", "Location not found"));
        } catch (Exception e) {
            System.err.println("Reverse geocoding error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("address", "Error retrieving address"));
        }
    }
}