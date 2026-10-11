package com.waste.management.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExtractedImageMetadata(
        BigDecimal exifLatitude,
        BigDecimal exifLongitude,
        OffsetDateTime exifCapturedAt,
        String cameraMake,
        String cameraModel,
        boolean hasExifGps,
        boolean hasExifDate
) {
    public static ExtractedImageMetadata empty() {
        return new ExtractedImageMetadata(null, null, null, null, null, false, false);
    }
}
