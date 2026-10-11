package com.waste.management.controller;

import com.waste.management.dto.VolunteerProfileResponse;
import com.waste.management.dto.VolunteerStatusRequest;
import com.waste.management.security.UserPrincipal;
import com.waste.management.service.VolunteerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/volunteer")
public class VolunteerController {

    private final VolunteerService volunteerService;

    public VolunteerController(VolunteerService volunteerService) {
        this.volunteerService = volunteerService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('VOLUNTEER', 'ADMIN')")
    public ResponseEntity<VolunteerProfileResponse> getStatus(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        VolunteerProfileResponse response = volunteerService.getProfile(currentUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/status")
    @PreAuthorize("hasAnyRole('VOLUNTEER', 'ADMIN')")
    public ResponseEntity<VolunteerProfileResponse> updateStatus(
            @Valid @RequestBody VolunteerStatusRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        VolunteerProfileResponse response = volunteerService.updateStatus(request, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('VOLUNTEER', 'ADMIN')")
    public ResponseEntity<VolunteerProfileResponse> getProfile(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        VolunteerProfileResponse response = volunteerService.getProfile(currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VolunteerProfileResponse>> getAvailableVolunteers() {
        List<VolunteerProfileResponse> list = volunteerService.getAvailableVolunteers();
        return ResponseEntity.ok(list);
    }
}
