package com.homeride.backend.config;

import com.google.maps.GeoApiContext; // <-- IMPORT ADDED
import org.springframework.beans.factory.annotation.Value; // <-- IMPORT ADDED
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class ApplicationConfig {

    // This will read the API key from your application.properties file
    @Value("${google.maps.api.key:}")
    private String apiKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // This bean creates the Google Maps client for your application
    @Bean
    public GeoApiContext geoApiContext() {
        return new GeoApiContext.Builder()
                .apiKey(apiKey)
                .build();
    }
}
