package com.waste.management.dto;

import com.waste.management.entity.ComplaintImage;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ComplaintImageResponse(
        UUID id,
        String imageUrl,
        String publicId,
        String imageType,
        UUID uploadedBy,
        OffsetDateTime uploadedAt
) {
    public static ComplaintImageResponse fromEntity(ComplaintImage img) {
        return new ComplaintImageResponse(
                img.getId(),
                img.getImageUrl(),
                img.getPublicId(),
                img.getImageType(),
                img.getUploadedBy() != null ? img.getUploadedBy().getId() : null,
                img.getUploadedAt()
        );
    }
}
