package com.waste.management.dto;

import com.waste.management.entity.VerificationResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VerificationResultResponse(
        UUID id,
        UUID complaintId,
        UUID assignmentId,
        UUID imageId,
        String verifierType,
        String verificationStatus,
        String checkType,
        Boolean isValidPlastic,
        BigDecimal confidenceScore,
        BigDecimal deviceLatitude,
        BigDecimal deviceLongitude,
        BigDecimal exifLatitude,
        BigDecimal exifLongitude,
        BigDecimal targetLatitude,
        BigDecimal targetLongitude,
        Double distanceMeters,
        Double distanceToleranceMeters,
        String checkReasons,
        String reviewDecision,
        String adminNotes,
        UUID reviewedBy,
        OffsetDateTime reviewedAt,
        OffsetDateTime verifiedAt
) {
    public static VerificationResultResponse fromEntity(VerificationResult entity) {
        return new VerificationResultResponse(
                entity.getId(),
                entity.getComplaint() != null ? entity.getComplaint().getId() : null,
                entity.getAssignment() != null ? entity.getAssignment().getId() : null,
                entity.getImage() != null ? entity.getImage().getId() : null,
                entity.getVerifierType(),
                entity.getVerificationStatus(),
                entity.getCheckType(),
                entity.getIsValidPlastic(),
                entity.getConfidenceScore(),
                entity.getDeviceLatitude(),
                entity.getDeviceLongitude(),
                entity.getExifLatitude(),
                entity.getExifLongitude(),
                entity.getTargetLatitude(),
                entity.getTargetLongitude(),
                entity.getDistanceMeters(),
                entity.getDistanceToleranceMeters(),
                entity.getCheckReasons(),
                entity.getReviewDecision(),
                entity.getAdminNotes(),
                entity.getReviewedBy() != null ? entity.getReviewedBy().getId() : null,
                entity.getReviewedAt(),
                entity.getVerifiedAt()
        );
    }
}
