package com.waste.management.dto;

import jakarta.validation.constraints.NotBlank;

public record ComplaintRejectRequest(
        @NotBlank(message = "Rejection reason is required")
        String rejectionReason
) {}
