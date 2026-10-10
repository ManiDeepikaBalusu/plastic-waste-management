package com.waste.management.dto;

import jakarta.validation.constraints.NotBlank;

public record ComplaintCancelRequest(
        @NotBlank(message = "Cancellation reason is required")
        String cancellationReason
) {}
