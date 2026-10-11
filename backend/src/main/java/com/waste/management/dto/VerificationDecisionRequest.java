package com.waste.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificationDecisionRequest(
        @NotBlank(message = "Decision is required")
        @Pattern(regexp = "^(APPROVE|REJECT)$", message = "Decision must be either 'APPROVE' or 'REJECT'")
        String decision,

        @NotBlank(message = "Mandatory reason must be provided for audit accountability")
        String reason
) {}
