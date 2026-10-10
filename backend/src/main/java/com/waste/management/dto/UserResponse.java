package com.waste.management.dto;

import com.waste.management.entity.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String role,
        Boolean isActive,
        OffsetDateTime createdAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getIsActive(),
                user.getCreatedAt()
        );
    }
}
