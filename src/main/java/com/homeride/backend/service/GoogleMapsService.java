package com.homeride.backend.service;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.homeride.backend.dto.TravelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.maps.DirectionsApiRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class GoogleMapsService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleMapsService.class);
    private final GeoApiContext geoApiContext;

    @Value("${google.maps.api.key:}")
    private String apiKey;

    private static final TravelInfo DEFAULT_TRAVEL_INFO = new TravelInfo(200, 180.0, "", "Default Route", new ArrayList<>());

    @Autowired
    public GoogleMapsService(GeoApiContext geoApiContext) {
        this.geoApiContext = geoApiContext;
    }

    /**
     * Get direct route distance (for pricing calculations)
     * Origin to destination ONLY, ignoring any stopovers
     */
    public double getDirectDistance(String origin, String destination) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("Google Maps API key is not configured. Returning default distance.");
            return 180.0;
        }
        try {
            DirectionsApiRequest request = DirectionsApi.newRequest(geoApiContext)
                    .origin(origin)
                    .destination(destination);

            DirectionsResult result = request.await();

            if (result.routes != null && result.routes.length > 0) {
                DirectionsRoute route = result.routes[0];
                long totalDistanceInMeters = Arrays.stream(route.legs)
                        .mapToLong(leg -> leg.distance.inMeters)
                        .sum();
                double distanceInKm = totalDistanceInMeters / 1000.0;

                logger.info("Direct Distance (for pricing): {} to {} = {:.2f}km",
                        origin, destination, distanceInKm);
                return distanceInKm;
            }
        } catch (Exception e) {
            logger.error("Error fetching direct distance from Google Maps API: {}", e.getMessage());
        }
        return DEFAULT_TRAVEL_INFO.getDistanceInKm();
    }

    /**
     * Get full route info WITH stopovers (for display/logistics)
     * This includes segment distances for each leg of the journey
     */
    public TravelInfo getTravelInfoWithStopovers(String origin, String destination, String[] stops) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("Google Maps API key is not configured. Returning default travel info.");
            return DEFAULT_TRAVEL_INFO;
        }
        try {
            DirectionsApiRequest request = DirectionsApi.newRequest(geoApiContext)
                    .origin(origin)
                    .destination(destination);

            if (stops != null && stops.length > 0) {
                request.waypoints(stops);
            }

            DirectionsResult result = request.await();

            if (result.routes != null && result.routes.length > 0) {
                DirectionsRoute route = result.routes[0];
                String polyline = route.overviewPolyline.getEncodedPath();
                String summary = route.summary;

                long totalDurationInSeconds = Arrays.stream(route.legs)
                        .mapToLong(leg -> leg.duration.inSeconds)
                        .sum();
                long totalDistanceInMeters = Arrays.stream(route.legs)
                        .mapToLong(leg -> leg.distance.inMeters)
                        .sum();

                int durationInMinutes = (int) (totalDurationInSeconds / 60);
                double distanceInKm = totalDistanceInMeters / 1000.0;

                // Calculate segment distances (these are the actual leg distances with stopovers)
                List<Double> segmentDistances = new ArrayList<>();
                for (int i = 0; i < route.legs.length; i++) {
                    double segmentDistanceKm = route.legs[i].distance.inMeters / 1000.0;
                    segmentDistances.add(segmentDistanceKm);
                    logger.debug("Segment {}: {} → {} = {:.2f}km",
                            i + 1,
                            route.legs[i].startAddress,
                            route.legs[i].endAddress,
                            segmentDistanceKm);
                }

                logger.info("Route with Stopovers - Total Distance: {:.2f}km, Duration: {}min, Segments: {}",
                        distanceInKm, durationInMinutes, segmentDistances.size());

                return new TravelInfo(durationInMinutes, distanceInKm, polyline, summary, segmentDistances);
            }
        } catch (Exception e) {
            logger.error("Error fetching travel info with stopovers from Google Maps API: {}", e.getMessage());
        }
        return DEFAULT_TRAVEL_INFO;
    }

    /**
     * Get complete travel info (kept for backward compatibility)
     * Now combines direct distance for pricing with stopover route for logistics
     */
    public TravelInfo getTravelInfo(String origin, String destination, String[] stops) {
        return getTravelInfoWithStopovers(origin, destination, stops);
    }

    public LatLng geocodeAddress(String address) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("Google Maps API key is not configured. Geocoding disabled.");
            return null;
        }
        try {
            GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, address).await();
            if (results != null && results.length > 0) {
                return results[0].geometry.location;
            }
        } catch (Exception e) {
            logger.error("Error during geocoding for address '{}': {}", address, e.getMessage());
        }
        return null;
    }

    public String reverseGeocode(LatLng location) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("Google Maps API key is not configured. Reverse geocoding disabled.");
            return "Service unavailable";
        }
        try {
            GeocodingResult[] results = GeocodingApi.reverseGeocode(geoApiContext, location).await();
            if (results != null && results.length > 0) {
                return results[0].formattedAddress;
            }
        } catch (Exception e) {
            logger.error("Error during reverse geocoding for location '{}': {}", location, e.getMessage());
        }
        return "Unknown location";
    }
}