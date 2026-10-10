package com.waste.management.dto;

import com.waste.management.entity.VolunteerProfile;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VolunteerProfileResponse(
        UUID id,
        UUID userId,
        String fullName,
        String email,
        String phone,
        Boolean isAvailable,
        BigDecimal currentLatitude,
        BigDecimal currentLongitude,
        Integer totalAssigned,
        Integer totalResolved,
        OffsetDateTime lastLocationUpdate
) {
    public static VolunteerProfileResponse fromEntity(VolunteerProfile profile) {
        return new VolunteerProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getFullName(),
                profile.getUser().getEmail(),
                profile.getUser().getPhone(),
                profile.getIsAvailable(),
                profile.getCurrentLatitude(),
                profile.getCurrentLongitude(),
                profile.getTotalAssigned(),
                profile.getTotalResolved(),
                profile.getLastLocationUpdate()
        );
    }
}
