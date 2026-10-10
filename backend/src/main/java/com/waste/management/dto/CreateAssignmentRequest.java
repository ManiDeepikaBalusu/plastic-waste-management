package com.waste.management.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateAssignmentRequest(
        @NotNull(message = "complaintId is required")
        UUID complaintId,

        @NotNull(message = "volunteerId is required")
        UUID volunteerId,

        String notes
) {}
