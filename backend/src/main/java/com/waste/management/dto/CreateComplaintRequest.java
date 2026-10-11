package com.waste.management.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CreateComplaintRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        String wasteType,

        String estimatedSize,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be greater than or equal to -90.0")
        @DecimalMax(value = "90.0", message = "Latitude must be less than or equal to 90.0")
        BigDecimal latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be greater than or equal to -180.0")
        @DecimalMax(value = "180.0", message = "Longitude must be less than or equal to 180.0")
        BigDecimal longitude,

        String addressText,

        List<String> imageUrls,

        @JsonAlias({"deviceLat", "device_latitude"})
        BigDecimal deviceLatitude,

        @JsonAlias({"deviceLon", "device_longitude", "deviceLng", "device_lng"})
        BigDecimal deviceLongitude,

        @JsonAlias({"device_captured_at", "capturedAt", "captured_at", "deviceTimestamp"})
        OffsetDateTime deviceCapturedAt,

        @JsonAlias({"capture_source", "source"})
        String captureSource,

        @JsonAlias({"device_accuracy", "accuracy"})
        Double deviceAccuracy,

        @JsonAlias({"device_gps_timestamp", "gpsTimestamp"})
        OffsetDateTime deviceGpsTimestamp,

        @JsonAlias({"file_last_modified_at", "lastModifiedAt"})
        OffsetDateTime fileLastModifiedAt
) {
    public CreateComplaintRequest(
            String title,
            String description,
            String wasteType,
            String estimatedSize,
            BigDecimal latitude,
            BigDecimal longitude,
            String addressText,
            List<String> imageUrls) {
        this(title, description, wasteType, estimatedSize, latitude, longitude, addressText, imageUrls, null, null, null, null, null, null, null);
    }

    public CreateComplaintRequest(
            String title,
            String description,
            String wasteType,
            String estimatedSize,
            BigDecimal latitude,
            BigDecimal longitude,
            String addressText,
            List<String> imageUrls,
            BigDecimal deviceLatitude,
            BigDecimal deviceLongitude,
            OffsetDateTime deviceCapturedAt,
            String captureSource) {
        this(title, description, wasteType, estimatedSize, latitude, longitude, addressText, imageUrls, deviceLatitude, deviceLongitude, deviceCapturedAt, captureSource, null, null, null);
    }
}
