package com.waste.management.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PhotoVerificationResultDto(
        String verificationStatus, // VERIFIED, NEEDS_REVIEW, REJECTED
        String checkType,          // INITIAL_REPORT, CLEANUP_RESOLUTION
        BigDecimal deviceLatitude,
        BigDecimal deviceLongitude,
        OffsetDateTime deviceCapturedAt,
        BigDecimal exifLatitude,
        BigDecimal exifLongitude,
        OffsetDateTime exifCapturedAt,
        String cameraMake,
        String cameraModel,
        String sha256Hash,
        Double distanceToTargetMeters,
        Double distanceDeviceToExifMeters,
        Double toleranceMeters,
        List<String> flags,
        double confidenceScore,
        boolean isDuplicate,
        boolean isLocationConsistent
) {}
