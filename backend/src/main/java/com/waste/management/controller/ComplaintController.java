package com.waste.management.controller;

import com.waste.management.dto.*;
import com.waste.management.security.UserPrincipal;
import com.waste.management.service.ComplaintService;
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
@RequestMapping("/api/v1/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ComplaintResponse> createComplaint(
            @Valid @RequestBody CreateComplaintRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ComplaintResponse response = complaintService.createComplaint(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/submit-with-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ComplaintResponse> createComplaintWithPhotos(
            @Valid @RequestPart("data") CreateComplaintRequest request,
            @RequestPart("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ComplaintResponse response = complaintService.createComplaintWithPhotos(request, files, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<ComplaintResponse> complaints = complaintService.getCitizenComplaints(currentUser);
        return ResponseEntity.ok(complaints);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ComplaintResponse> getComplaintById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ComplaintResponse response = complaintService.getComplaintById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ComplaintResponse>> getAllComplaints(
            @RequestParam(required = false) String status) {
        List<ComplaintResponse> list = complaintService.getAllComplaints(status);
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplaintResponse> markUnderReview(
            @PathVariable UUID id,
            @RequestParam(required = false) String remarks,
            @AuthenticationPrincipal UserPrincipal adminUser) {
        ComplaintResponse response = complaintService.markUnderReview(id, remarks, adminUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplaintResponse> verifyComplaint(
            @PathVariable UUID id,
            @RequestParam(required = false) String remarks,
            @AuthenticationPrincipal UserPrincipal adminUser) {
        ComplaintResponse response = complaintService.verifyComplaint(id, remarks, adminUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplaintResponse> rejectComplaint(
            @PathVariable UUID id,
            @Valid @RequestBody ComplaintRejectRequest request,
            @AuthenticationPrincipal UserPrincipal adminUser) {
        ComplaintResponse response = complaintService.rejectComplaint(id, request, adminUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CITIZEN') or hasRole('ADMIN')")
    public ResponseEntity<ComplaintResponse> cancelComplaint(
            @PathVariable UUID id,
            @Valid @RequestBody ComplaintCancelRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ComplaintResponse response = complaintService.cancelComplaint(id, request, currentUser);
        return ResponseEntity.ok(response);
    }
}
