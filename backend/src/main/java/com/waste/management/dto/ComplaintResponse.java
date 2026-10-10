package com.waste.management.dto;

import com.waste.management.entity.Complaint;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ComplaintResponse(
        UUID id,
        UUID citizenId,
        String citizenName,
        String title,
        String description,
        String wasteType,
        String estimatedSize,
        BigDecimal latitude,
        BigDecimal longitude,
        String addressText,
        String status,
        String cancellationReason,
        String rejectionReason,
        List<ComplaintImageResponse> images,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ComplaintResponse fromEntity(Complaint complaint) {
        List<ComplaintImageResponse> imageList = complaint.getImages() != null ?
                complaint.getImages().stream().map(ComplaintImageResponse::fromEntity).toList() :
                List.of();

        return new ComplaintResponse(
                complaint.getId(),
                complaint.getCitizen() != null ? complaint.getCitizen().getId() : null,
                complaint.getCitizen() != null ? complaint.getCitizen().getFullName() : null,
                complaint.getTitle(),
                complaint.getDescription(),
                complaint.getWasteType(),
                complaint.getEstimatedSize(),
                complaint.getLatitude(),
                complaint.getLongitude(),
                complaint.getAddressText(),
                complaint.getStatus(),
                complaint.getCancellationReason(),
                complaint.getRejectionReason(),
                imageList,
                complaint.getCreatedAt(),
                complaint.getUpdatedAt()
        );
    }
}
