package com.waste.management.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
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

        List<String> imageUrls
) {}
