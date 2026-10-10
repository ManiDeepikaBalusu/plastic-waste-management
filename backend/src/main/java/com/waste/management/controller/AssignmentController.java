package com.waste.management.controller;

import com.waste.management.dto.AssignmentResponse;
import com.waste.management.dto.CreateAssignmentRequest;
import com.waste.management.security.UserPrincipal;
import com.waste.management.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssignmentResponse> assignComplaint(
            @Valid @RequestBody CreateAssignmentRequest request,
            @AuthenticationPrincipal UserPrincipal adminUser) {
        AssignmentResponse response = assignmentService.assignComplaint(request, adminUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-tasks")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<List<AssignmentResponse>> getMyAssignments(
            @AuthenticationPrincipal UserPrincipal volunteerUser) {
        List<AssignmentResponse> list = assignmentService.getMyAssignments(volunteerUser);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AssignmentResponse> getAssignmentById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AssignmentResponse response = assignmentService.getAssignmentById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<AssignmentResponse> acceptAssignment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal volunteerUser) {
        AssignmentResponse response = assignmentService.acceptAssignment(id, volunteerUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/decline")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<AssignmentResponse> declineAssignment(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserPrincipal volunteerUser) {
        AssignmentResponse response = assignmentService.declineAssignment(id, reason, volunteerUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<AssignmentResponse> startAssignment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal volunteerUser) {
        AssignmentResponse response = assignmentService.startAssignment(id, volunteerUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{id}/resolve", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<AssignmentResponse> resolveAssignment(
            @PathVariable UUID id,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal volunteerUser) {
        AssignmentResponse response = assignmentService.resolveAssignmentWithProof(id, notes, files, volunteerUser);
        return ResponseEntity.ok(response);
    }
}
