package com.waste.management.dto;

public record MediaUploadResponse(
        String imageUrl,
        String publicId,
        String message
) {}
