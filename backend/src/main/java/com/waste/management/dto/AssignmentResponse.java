package com.waste.management.dto;

import com.waste.management.entity.Assignment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID complaintId,
        String complaintTitle,
        String complaintStatus,
        BigDecimal latitude,
        BigDecimal longitude,
        String addressText,
        UUID volunteerId,
        String volunteerName,
        String status,
        String notes,
        OffsetDateTime assignedAt,
        OffsetDateTime acceptedAt,
        OffsetDateTime completedAt,
        String verificationStatus,
        String verificationNotes,
        List<String> resolutionProofUrls
) {
    public AssignmentResponse(
            UUID id, UUID complaintId, String complaintTitle, String complaintStatus,
            BigDecimal latitude, BigDecimal longitude, String addressText,
            UUID volunteerId, String volunteerName, String status, String notes,
            OffsetDateTime assignedAt, OffsetDateTime acceptedAt, OffsetDateTime completedAt,
            String verificationStatus, String verificationNotes) {
        this(id, complaintId, complaintTitle, complaintStatus, latitude, longitude, addressText,
                volunteerId, volunteerName, status, notes, assignedAt, acceptedAt, completedAt,
                verificationStatus, verificationNotes, List.of());
    }

    public static AssignmentResponse fromEntity(Assignment a) {
        List<String> proofUrls = List.of();
        if (a.getComplaint() != null && a.getComplaint().getImages() != null) {
            proofUrls = a.getComplaint().getImages().stream()
                    .filter(img -> "RESOLUTION_PROOF".equalsIgnoreCase(img.getImageType()))
                    .map(com.waste.management.entity.ComplaintImage::getImageUrl)
                    .toList();
        }

        return new AssignmentResponse(
                a.getId(),
                a.getComplaint().getId(),
                a.getComplaint().getTitle(),
                a.getComplaint().getStatus(),
                a.getComplaint().getLatitude(),
                a.getComplaint().getLongitude(),
                a.getComplaint().getAddressText(),
                a.getVolunteer().getId(),
                a.getVolunteer().getFullName(),
                a.getStatus(),
                a.getNotes(),
                a.getAssignedAt(),
                a.getAcceptedAt(),
                a.getCompletedAt(),
                a.getVerificationStatus(),
                a.getVerificationNotes(),
                proofUrls
        );
    }
}
