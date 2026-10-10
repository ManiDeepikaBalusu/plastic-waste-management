package com.waste.management.dto;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(Instant.now().toString(), status, error, message, path, null);
    }

    public static ApiErrorResponse of(int status, String error, String message, String path, Map<String, String> validationErrors) {
        return new ApiErrorResponse(Instant.now().toString(), status, error, message, path, validationErrors);
    }
}
