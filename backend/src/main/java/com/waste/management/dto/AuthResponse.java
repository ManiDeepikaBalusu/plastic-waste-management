package com.waste.management.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        UserResponse user,
        String message
) {}
