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
        boolean isLocationConsistent,
        String coordinateSource,
        String timestampSource,
        Double deviceGpsAccuracy,
        String ocrWatermarkText,
        String ocrWatermarkStatus,
        BigDecimal ocrLatitude,
        BigDecimal ocrLongitude,
        String ocrTimestamp,
        BigDecimal browserUploadLatitude,
        BigDecimal browserUploadLongitude
) {
    public PhotoVerificationResultDto(
            String verificationStatus,
            String checkType,
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
            boolean isLocationConsistent) {
        this(verificationStatus, checkType, deviceLatitude, deviceLongitude, deviceCapturedAt,
                exifLatitude, exifLongitude, exifCapturedAt, cameraMake, cameraModel, sha256Hash,
                distanceToTargetMeters, distanceDeviceToExifMeters, toleranceMeters, flags,
                confidenceScore, isDuplicate, isLocationConsistent, null, null, null, null, null,
                null, null, null, null, null);
    }

    public PhotoVerificationResultDto(
            String verificationStatus,
            String checkType,
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
            boolean isLocationConsistent,
            String coordinateSource,
            String timestampSource,
            Double deviceGpsAccuracy,
            String ocrWatermarkText,
            String ocrWatermarkStatus) {
        this(verificationStatus, checkType, deviceLatitude, deviceLongitude, deviceCapturedAt,
                exifLatitude, exifLongitude, exifCapturedAt, cameraMake, cameraModel, sha256Hash,
                distanceToTargetMeters, distanceDeviceToExifMeters, toleranceMeters, flags,
                confidenceScore, isDuplicate, isLocationConsistent, coordinateSource, timestampSource,
                deviceGpsAccuracy, ocrWatermarkText, ocrWatermarkStatus, null, null, null, null, null);
    }
}
