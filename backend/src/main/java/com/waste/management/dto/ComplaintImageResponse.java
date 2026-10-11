package com.waste.management.dto;

import com.waste.management.entity.ComplaintImage;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ComplaintImageResponse(
        UUID id,
        String imageUrl,
        String publicId,
        String imageType,
        UUID uploadedBy,
        OffsetDateTime uploadedAt,
        String verificationStatus,
        String verificationFlags,
        Double distanceToReportMeters,
        String captureSource,
        BigDecimal deviceLatitude,
        BigDecimal deviceLongitude,
        BigDecimal exifLatitude,
        BigDecimal exifLongitude
) {
    public static ComplaintImageResponse fromEntity(ComplaintImage img) {
        return new ComplaintImageResponse(
                img.getId(),
                img.getImageUrl(),
                img.getPublicId(),
                img.getImageType(),
                img.getUploadedBy() != null ? img.getUploadedBy().getId() : null,
                img.getUploadedAt(),
                img.getVerificationStatus(),
                img.getVerificationFlags(),
                img.getDistanceToReportMeters(),
                img.getCaptureSource(),
                img.getDeviceLatitude(),
                img.getDeviceLongitude(),
                img.getExifLatitude(),
                img.getExifLongitude()
        );
    }
}
