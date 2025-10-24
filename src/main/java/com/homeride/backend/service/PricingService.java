package com.homeride.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PricingService {

    private static final Logger logger = LoggerFactory.getLogger(PricingService.class);

    // --- Pricing Constants ---
    private static final double BASE_FARE = 50.0;
    private static final double RATE_PER_KM_SHORT = 2.2;    // < 100km
    private static final double RATE_PER_KM_MEDIUM = 2.0;   // 100-300km
    private static final double RATE_PER_KM_LONG = 1.8;     // > 300km

    private static final double MIN_RECOMMENDED_PRICE = 50.0;
    private static final double MIN_ABSOLUTE_PRICE = 50.0;
    private static final double MIN_SEGMENT_PRICE = 30.0;

    private static final double TOTAL_MIN_FACTOR = 0.6;
    private static final double TOTAL_MAX_FACTOR = 1.7;
    private static final double SEGMENT_MIN_FACTOR = 0.5;
    private static final double SEGMENT_MAX_FACTOR = 2.0;

    private static final double MIN_RANGE_PRICE = 100.0;
    private static final double MIN_SEGMENT_RANGE_PRICE = 50.0;

    /**
     * Calculates the base recommended price for any distance
     * Uses distance-based tiers only, NO hardcoded routes
     */
    private double calculateRecommendedPrice(double distanceKm) {
        if (distanceKm <= 0) return MIN_RECOMMENDED_PRICE;

        double rate;
        if (distanceKm < 100) {
            rate = RATE_PER_KM_SHORT;
        } else if (distanceKm <= 300) {
            rate = RATE_PER_KM_MEDIUM;
        } else {
            rate = RATE_PER_KM_LONG;
        }

        double calculatedPrice = BASE_FARE + (distanceKm * rate);
        return Math.max(MIN_RECOMMENDED_PRICE, roundToNearest10(calculatedPrice));
    }

    /**
     * Rounds a price to the nearest 10
     */
    private double roundToNearest10(double price) {
        return Math.round(price / 10.0) * 10.0;
    }

    /**
     * Gets price range for the TOTAL ride (origin to destination)
     * Based ONLY on total distance, completely dynamic
     */
    public PriceRange getTotalPriceRange(double totalDistanceKm) {
        double recommended = calculateRecommendedPrice(totalDistanceKm);

        double minPrice = roundToNearest10(recommended * TOTAL_MIN_FACTOR);
        minPrice = Math.max(MIN_ABSOLUTE_PRICE, minPrice);

        double maxPrice = roundToNearest10(recommended * TOTAL_MAX_FACTOR);
        maxPrice = Math.max(minPrice + MIN_RANGE_PRICE, maxPrice);

        logger.info("Total Price Range for {}km: Min={}, Recommended={}, Max={}",
                totalDistanceKm, minPrice, recommended, maxPrice);

        return new PriceRange(minPrice, maxPrice, recommended);
    }

    /**
     * Gets price range for a SINGLE SEGMENT (independent calculation)
     * Based ONLY on segment distance, completely dynamic
     */
    public PriceRange getSegmentPriceRange(double segmentDistanceKm) {
        double recommended = calculateRecommendedPrice(segmentDistanceKm);

        double minPrice = roundToNearest10(recommended * SEGMENT_MIN_FACTOR);
        minPrice = Math.max(MIN_SEGMENT_PRICE, minPrice);

        double maxPrice = roundToNearest10(recommended * SEGMENT_MAX_FACTOR);
        maxPrice = Math.max(minPrice + MIN_SEGMENT_RANGE_PRICE, maxPrice);

        logger.debug("Segment Price Range for {}km: Min={}, Recommended={}, Max={}",
                segmentDistanceKm, minPrice, recommended, maxPrice);

        return new PriceRange(minPrice, maxPrice, recommended);
    }

    /**
     * Inner class to hold price range data
     */
    public static class PriceRange {
        public final double minPrice;
        public final double maxPrice;
        public final double recommendedPrice;

        public PriceRange(double min, double max, double recommended) {
            this.minPrice = min;
            this.maxPrice = max;
            this.recommendedPrice = recommended;
        }

        @Override
        public String toString() {
            return String.format("PriceRange{min=%.0f, rec=%.0f, max=%.0f}",
                    minPrice, recommendedPrice, maxPrice);
        }
    }
}