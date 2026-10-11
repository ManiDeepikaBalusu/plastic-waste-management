package com.waste.management.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class GeoDistanceCalculatorTests {

    private GeoDistanceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new GeoDistanceCalculator();
    }

    @Test
    @DisplayName("1. Identical double coordinates return exactly 0.0 meters")
    void testIdenticalDoubleCoordinatesReturnZero() {
        double lat = 12.971598;
        double lon = 77.594563;

        Double distance = calculator.calculateDistanceMeters(lat, lon, lat, lon);

        assertNotNull(distance);
        assertEquals(0.0, distance, 0.0001, "Distance between identical coordinates must be 0.0 meters");
    }

    @Test
    @DisplayName("2. Identical BigDecimal coordinates return exactly 0.0 meters")
    void testIdenticalBigDecimalCoordinatesReturnZero() {
        BigDecimal lat = new BigDecimal("12.971600");
        BigDecimal lon = new BigDecimal("77.594600");

        Double distance = calculator.calculateDistanceMeters(lat, lon, lat, lon);

        assertNotNull(distance);
        assertEquals(0.0, distance, 0.0001, "Distance between identical BigDecimal coordinates must be 0.0 meters");
    }

    @Test
    @DisplayName("3. Known nearby coordinates return accurate distance within tolerance")
    void testNearbyCoordinatesDistanceCalculation() {
        // Points ~60 meters apart in Bangalore
        double lat1 = 12.971600;
        double lon1 = 77.594600;
        double lat2 = 12.972000;
        double lon2 = 77.595000;

        Double distance = calculator.calculateDistanceMeters(lat1, lon1, lat2, lon2);

        assertNotNull(distance);
        assertTrue(distance > 50.0 && distance < 75.0, "Distance should be ~64 meters, got: " + distance);
    }

    @Test
    @DisplayName("4. Coordinates far apart calculate accurate distance beyond standard tolerance")
    void testFarCoordinatesCalculation() {
        // Bangalore to Mysore (~128 km)
        double bangaloreLat = 12.9716;
        double bangaloreLon = 77.5946;
        double mysoreLat = 12.2958;
        double mysoreLon = 76.6394;

        Double distance = calculator.calculateDistanceMeters(bangaloreLat, bangaloreLon, mysoreLat, mysoreLon);

        assertNotNull(distance);
        assertTrue(distance > 120000.0 && distance < 140000.0, "Distance should be ~128 km, got: " + distance);
    }

    @Test
    @DisplayName("5. Null coordinate inputs return null gracefully")
    void testNullCoordinatesReturnNull() {
        BigDecimal validLat = new BigDecimal("12.9716");
        BigDecimal validLon = new BigDecimal("77.5946");

        assertNull(calculator.calculateDistanceMeters(null, validLon, validLat, validLon));
        assertNull(calculator.calculateDistanceMeters(validLat, null, validLat, validLon));
        assertNull(calculator.calculateDistanceMeters(validLat, validLon, null, validLon));
        assertNull(calculator.calculateDistanceMeters(validLat, validLon, validLat, null));
        assertNull(calculator.calculateDistanceMeters(null, null, null, null));
    }

    @Test
    @DisplayName("6. Out-of-bounds latitude returns null and invalid")
    void testOutOfBoundsLatitude() {
        assertFalse(calculator.isValidCoordinate(91.0, 77.5));
        assertFalse(calculator.isValidCoordinate(-90.1, 77.5));
        assertNull(calculator.calculateDistanceMeters(95.0, 77.5, 12.9, 77.5));
    }

    @Test
    @DisplayName("7. Out-of-bounds longitude returns null and invalid")
    void testOutOfBoundsLongitude() {
        assertFalse(calculator.isValidCoordinate(12.9, 181.0));
        assertFalse(calculator.isValidCoordinate(12.9, -180.1));
        assertNull(calculator.calculateDistanceMeters(12.9, 190.0, 12.9, 77.5));
    }

    @Test
    @DisplayName("8. Swapped coordinates (lat > 90 and lon <= 90) detected correctly")
    void testSwappedCoordinatesDetection() {
        // Longitude accidentally put in latitude: 120.5 > 90.0
        assertTrue(calculator.areCoordinatesSwapped(120.5, 12.9));
        assertTrue(calculator.areCoordinatesSwapped(new BigDecimal("120.5"), new BigDecimal("12.9")));
        assertFalse(calculator.areCoordinatesSwapped(12.9, 77.5));
        assertNull(calculator.calculateDistanceMeters(120.5, 12.9, 12.9, 77.5));
    }

    @Test
    @DisplayName("9. NaN and Infinite coordinates handled safely without throwing exceptions")
    void testNaNAndInfiniteCoordinates() {
        assertFalse(calculator.isValidCoordinate(Double.NaN, 77.5));
        assertFalse(calculator.isValidCoordinate(12.9, Double.POSITIVE_INFINITY));
        assertNull(calculator.calculateDistanceMeters(Double.NaN, 77.5, 12.9, 77.5));
    }

    @Test
    @DisplayName("10. Sub-millimeter micro-differences are rounded or clamped to 0.0")
    void testSubMillimeterClamping() {
        double lat1 = 12.9716000000;
        double lon1 = 77.5946000000;
        double lat2 = 12.971600000001; // < 1 nanometer difference
        double lon2 = 77.5946000000;

        Double distance = calculator.calculateDistanceMeters(lat1, lon1, lat2, lon2);

        assertNotNull(distance);
        assertEquals(0.0, distance, 0.001, "Microscopic floating differences must be treated as 0.0m");
    }
}
