package com.homeride.backend.service;

import com.google.maps.GeoApiContext;
import com.google.maps.PlacesApi;
import com.google.maps.model.AutocompletePrediction;
import com.google.maps.model.PlaceAutocompleteType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GooglePlacesService {

    private static final Logger logger = LoggerFactory.getLogger(GooglePlacesService.class);
    private final GeoApiContext geoApiContext;

    @Value("${google.maps.api.key:}")
    private String apiKey;

    @Autowired
    public GooglePlacesService(GeoApiContext geoApiContext) {
        this.geoApiContext = geoApiContext;
    }

    public List<String> getAutocompleteSuggestions(String query) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("Google Places API key is not configured. Autocomplete disabled.");
            return Collections.emptyList();
        }

        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            AutocompletePrediction[] predictions = PlacesApi
                    .queryAutocomplete(geoApiContext, query)
                    .await();

            if (predictions == null || predictions.length == 0) {
                return Collections.emptyList();
            }

            return Arrays.stream(predictions)
                    .map(p -> p.description)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error fetching autocomplete suggestions: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}