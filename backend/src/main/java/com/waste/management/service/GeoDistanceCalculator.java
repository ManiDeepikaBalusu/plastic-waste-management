package com.waste.management.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class GeoDistanceCalculator {

    private static final double EARTH_RADIUS_METERS = 6371000.0; // Earth mean radius in meters

    /**
     * Calculates the great-circle distance between two GPS coordinates in meters using the Haversine formula.
     * Returns null if any coordinate is null.
     */
    public Double calculateDistanceMeters(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return null;
        }
        return calculateDistanceMeters(lat1.doubleValue(), lon1.doubleValue(), lat2.doubleValue(), lon2.doubleValue());
    }

    public Double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return Math.round(EARTH_RADIUS_METERS * c * 10.0) / 10.0; // round to 1 decimal place
    }
}
