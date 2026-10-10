package com.waste.management.dto;

import com.waste.management.entity.Assignment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
        OffsetDateTime completedAt
) {
    public static AssignmentResponse fromEntity(Assignment a) {
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
                a.getCompletedAt()
        );
    }
}
