package com.waste.management.service;

import com.waste.management.dto.VolunteerProfileResponse;
import com.waste.management.dto.VolunteerStatusRequest;
import com.waste.management.entity.User;
import com.waste.management.entity.VolunteerProfile;
import com.waste.management.exception.ResourceNotFoundException;
import com.waste.management.repository.UserRepository;
import com.waste.management.repository.VolunteerProfileRepository;
import com.waste.management.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class VolunteerService {

    private final VolunteerProfileRepository volunteerProfileRepository;
    private final UserRepository userRepository;

    public VolunteerService(VolunteerProfileRepository volunteerProfileRepository, UserRepository userRepository) {
        this.volunteerProfileRepository = volunteerProfileRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public VolunteerProfileResponse updateStatus(VolunteerStatusRequest request, UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUser.getId()));

        VolunteerProfile profile = volunteerProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    VolunteerProfile newProf = new VolunteerProfile(user);
                    return volunteerProfileRepository.save(newProf);
                });

        if (request.isAvailable() != null) {
            profile.setIsAvailable(request.isAvailable());
        }

        if (request.currentLatitude() != null && request.currentLongitude() != null) {
            profile.setCurrentLatitude(request.currentLatitude());
            profile.setCurrentLongitude(request.currentLongitude());
            profile.setLastLocationUpdate(OffsetDateTime.now());
        }

        VolunteerProfile saved = volunteerProfileRepository.save(profile);
        return VolunteerProfileResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public VolunteerProfileResponse getProfile(UserPrincipal currentUser) {
        VolunteerProfile profile = volunteerProfileRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> {
                    User user = userRepository.findById(currentUser.getId())
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    return new VolunteerProfile(user);
                });

        return VolunteerProfileResponse.fromEntity(profile);
    }

    @Transactional(readOnly = true)
    public List<VolunteerProfileResponse> getAvailableVolunteers() {
        List<User> volunteers = userRepository.findAll().stream()
                .filter(u -> "VOLUNTEER".equalsIgnoreCase(u.getRole()))
                .toList();

        return volunteers.stream().map(vol -> {
            VolunteerProfile profile = volunteerProfileRepository.findByUserId(vol.getId())
                    .orElseGet(() -> new VolunteerProfile(vol));
            return VolunteerProfileResponse.fromEntity(profile);
        }).toList();
    }
}
