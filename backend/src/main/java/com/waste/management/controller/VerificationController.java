package com.waste.management.controller;

import com.waste.management.dto.VerificationDecisionRequest;
import com.waste.management.dto.VerificationResultResponse;
import com.waste.management.security.UserPrincipal;
import com.waste.management.service.PhotoVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/verifications")
public class VerificationController {

    private final PhotoVerificationService photoVerificationService;

    public VerificationController(PhotoVerificationService photoVerificationService) {
        this.photoVerificationService = photoVerificationService;
    }

    @GetMapping("/complaint/{complaintId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VerificationResultResponse>> getVerificationsForComplaint(
            @PathVariable UUID complaintId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<VerificationResultResponse> results = photoVerificationService.getVerificationsForComplaint(complaintId, currentUser);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VerificationResultResponse>> getVerificationsForAssignment(
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<VerificationResultResponse> results = photoVerificationService.getVerificationsForAssignment(assignmentId, currentUser);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VerificationResultResponse> recordDecision(
            @PathVariable UUID id,
            @Valid @RequestBody VerificationDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal adminUser) {
        VerificationResultResponse response = photoVerificationService.processAdminDecision(id, request, adminUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/re-evaluate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VerificationResultResponse> reEvaluate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal adminUser) {
        VerificationResultResponse response = photoVerificationService.reEvaluateVerification(id, adminUser);
        return ResponseEntity.ok(response);
    }
}
