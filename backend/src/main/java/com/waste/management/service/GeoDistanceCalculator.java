package com.waste.management.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class GeoDistanceCalculator {

    private static final double EARTH_RADIUS_METERS = 6371000.0; // Earth mean radius in meters

    /**
     * Checks if a coordinate pair (latitude, longitude) is geographically valid.
     * Latitude must be in [-90.0, 90.0] and longitude in [-180.0, 180.0].
     */
    public boolean isValidCoordinate(Double lat, Double lon) {
        if (lat == null || lon == null) {
            return false;
        }
        if (lat.isNaN() || lon.isNaN() || lat.isInfinite() || lon.isInfinite()) {
            return false;
        }
        return lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0;
    }

    public boolean isValidCoordinate(BigDecimal lat, BigDecimal lon) {
        if (lat == null || lon == null) {
            return false;
        }
        return isValidCoordinate(lat.doubleValue(), lon.doubleValue());
    }

    /**
     * Checks if coordinates appear to be accidentally swapped (e.g., latitude is > 90 but <= 180 while longitude is <= 90).
     */
    public boolean areCoordinatesSwapped(Double lat, Double lon) {
        if (lat == null || lon == null) {
            return false;
        }
        return (Math.abs(lat) > 90.0 && Math.abs(lat) <= 180.0) && (Math.abs(lon) <= 90.0);
    }

    public boolean areCoordinatesSwapped(BigDecimal lat, BigDecimal lon) {
        if (lat == null || lon == null) {
            return false;
        }
        return areCoordinatesSwapped(lat.doubleValue(), lon.doubleValue());
    }

    /**
     * Calculates the great-circle distance between two GPS coordinates in meters using the Haversine formula.
     * Returns null if any coordinate is null or invalid.
     */
    public Double calculateDistanceMeters(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return null;
        }
        if (!isValidCoordinate(lat1, lon1) || !isValidCoordinate(lat2, lon2)) {
            return null;
        }
        return calculateDistanceMeters(lat1.doubleValue(), lon1.doubleValue(), lat2.doubleValue(), lon2.doubleValue());
    }

    public Double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        if (!isValidCoordinate(lat1, lon1) || !isValidCoordinate(lat2, lon2)) {
            return null;
        }

        // Exact match: distance is exactly 0.0 meters
        if (Double.compare(lat1, lat2) == 0 && Double.compare(lon1, lon2) == 0) {
            return 0.0;
        }

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        // Clamp 'a' to [0.0, 1.0] to guard against floating-point inaccuracies
        a = Math.max(0.0, Math.min(1.0, a));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_METERS * c;

        // Sub-millimeter differences are treated as 0.0 meters
        if (distance < 0.001) {
            return 0.0;
        }

        return Math.round(distance * 10.0) / 10.0; // round to 1 decimal place
    }
}
