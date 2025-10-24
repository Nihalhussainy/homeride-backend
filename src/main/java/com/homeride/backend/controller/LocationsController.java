package com.homeride.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/locations")
public class LocationsController {

    // This is a free, open-source alternative to Google Places API.
    // It's great for development and non-commercial projects.
    private static final String NOMINATIM_API_URL = "https://nominatim.openstreetmap.org/search";

    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> getAutocompleteSuggestions(@RequestParam String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        RestTemplate restTemplate = new RestTemplate();
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(NOMINATIM_API_URL)
                .queryParam("q", query)
                .queryParam("format", "json")
                .queryParam("limit", 5); // Limit to 5 suggestions

        try {
            String response = restTemplate.getForObject(uriBuilder.toUriString(), String.class);

            // Manually parse the JSON to extract display names
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            List<String> suggestions = root.findValuesAsText("display_name");

            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            // Log the error in a real application
            System.err.println("Error fetching autocomplete suggestions: " + e.getMessage());
            return ResponseEntity.status(500).body(Collections.singletonList("Error fetching locations"));
        }
    }
}
