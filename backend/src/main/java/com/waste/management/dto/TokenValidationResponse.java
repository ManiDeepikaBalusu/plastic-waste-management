package com.waste.management.dto;

public record TokenValidationResponse(
        boolean valid,
        String message,
        String emailMasked
) {
    public static TokenValidationResponse valid(String emailMasked) {
        return new TokenValidationResponse(true, "Token is valid", emailMasked);
    }

    public static TokenValidationResponse invalid(String message) {
        return new TokenValidationResponse(false, message, null);
    }
}
