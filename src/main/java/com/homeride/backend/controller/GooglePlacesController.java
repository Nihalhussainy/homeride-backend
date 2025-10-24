package com.homeride.backend.controller;

import com.homeride.backend.service.GooglePlacesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/places")
public class GooglePlacesController {

    private final GooglePlacesService googlePlacesService;

    @Autowired
    public GooglePlacesController(GooglePlacesService googlePlacesService) {
        this.googlePlacesService = googlePlacesService;
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> getAutocompleteSuggestions(@RequestParam String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<String> suggestions = googlePlacesService.getAutocompleteSuggestions(query);

            // Always return a list, never null
            if (suggestions == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            System.out.println("Returning " + suggestions.size() + " suggestions for query: " + query);
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            System.err.println("Error in autocomplete controller: " + e.getMessage());
            e.printStackTrace();
            // Return empty list instead of error to prevent frontend crashes
            return ResponseEntity.ok(Collections.emptyList());
        }
    }
}