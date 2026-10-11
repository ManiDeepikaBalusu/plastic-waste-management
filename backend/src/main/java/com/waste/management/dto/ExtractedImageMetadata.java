package com.waste.management.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExtractedImageMetadata(
        BigDecimal exifLatitude,
        BigDecimal exifLongitude,
        OffsetDateTime exifCapturedAt,
        String cameraMake,
        String cameraModel,
        String software,
        String imageDescription,
        String userComment,
        boolean hasExifGps,
        boolean hasExifDate,
        boolean hasWatermarkIndicator
) {
    public ExtractedImageMetadata(
            BigDecimal exifLatitude,
            BigDecimal exifLongitude,
            OffsetDateTime exifCapturedAt,
            String cameraMake,
            String cameraModel,
            boolean hasExifGps,
            boolean hasExifDate) {
        this(exifLatitude, exifLongitude, exifCapturedAt, cameraMake, cameraModel, null, null, null, hasExifGps, hasExifDate, false);
    }

    public static ExtractedImageMetadata empty() {
        return new ExtractedImageMetadata(null, null, null, null, null, null, null, null, false, false, false);
    }
}
