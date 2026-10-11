package com.waste.management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waste.management.dto.ExtractedImageMetadata;
import com.waste.management.dto.PhotoVerificationResultDto;
import com.waste.management.entity.Assignment;
import com.waste.management.entity.Complaint;
import com.waste.management.entity.ComplaintImage;
import com.waste.management.entity.User;
import com.waste.management.entity.VerificationResult;
import com.waste.management.dto.VerificationDecisionRequest;
import com.waste.management.dto.VerificationResultResponse;
import com.waste.management.entity.ProgressUpdate;
import com.waste.management.exception.ResourceNotFoundException;
import com.waste.management.repository.*;
import com.waste.management.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class PhotoVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(PhotoVerificationService.class);

    private final ExifMetadataExtractor exifExtractor;
    private final GeoDistanceCalculator distanceCalculator;
    private final ComplaintImageRepository complaintImageRepository;
    private final VerificationResultRepository verificationResultRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final ComplaintRepository complaintRepository;
    private final ProgressUpdateRepository progressUpdateRepository;
    private final NotificationService notificationService;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.verification.location-tolerance-meters:200.0}")
    private double locationToleranceMeters;

    @Value("${app.verification.cleanup-tolerance-meters:200.0}")
    private double cleanupToleranceMeters;

    @Value("${app.verification.time-tolerance-hours:24}")
    private long timeToleranceHours;

    public PhotoVerificationService(
            ExifMetadataExtractor exifExtractor,
            GeoDistanceCalculator distanceCalculator,
            ComplaintImageRepository complaintImageRepository,
            VerificationResultRepository verificationResultRepository,
            UserRepository userRepository,
            AssignmentRepository assignmentRepository,
            ComplaintRepository complaintRepository,
            ProgressUpdateRepository progressUpdateRepository,
            NotificationService notificationService,
            VolunteerProfileRepository volunteerProfileRepository,
            ObjectMapper objectMapper) {
        this.exifExtractor = exifExtractor;
        this.distanceCalculator = distanceCalculator;
        this.complaintImageRepository = complaintImageRepository;
        this.verificationResultRepository = verificationResultRepository;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.complaintRepository = complaintRepository;
        this.progressUpdateRepository = progressUpdateRepository;
        this.notificationService = notificationService;
        this.volunteerProfileRepository = volunteerProfileRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Verifies an initial report photo submitted by a citizen.
     */
    @Transactional
    public PhotoVerificationResultDto verifyInitialPhoto(
            MultipartFile file,
            Complaint complaint,
            ComplaintImage complaintImage,
            BigDecimal deviceLat,
            BigDecimal deviceLon,
            OffsetDateTime deviceCapturedAt,
            String captureSource,
            User citizen) {

        return evaluateAndRecord(
                file,
                complaint,
                null,
                complaintImage,
                complaint.getLatitude(),
                complaint.getLongitude(),
                deviceLat,
                deviceLon,
                deviceCapturedAt,
                captureSource,
                "INITIAL_REPORT",
                locationToleranceMeters,
                citizen
        );
    }

    /**
     * Verifies a cleanup proof photo submitted by a volunteer.
     */
    @Transactional
    public PhotoVerificationResultDto verifyCleanupPhoto(
            MultipartFile file,
            Complaint complaint,
            Assignment assignment,
            ComplaintImage complaintImage,
            BigDecimal deviceLat,
            BigDecimal deviceLon,
            OffsetDateTime deviceCapturedAt,
            String captureSource,
            User volunteer) {

        return evaluateAndRecord(
                file,
                complaint,
                assignment,
                complaintImage,
                complaint.getLatitude(),
                complaint.getLongitude(),
                deviceLat,
                deviceLon,
                deviceCapturedAt,
                captureSource,
                "CLEANUP_RESOLUTION",
                cleanupToleranceMeters,
                volunteer
        );
    }

    private PhotoVerificationResultDto evaluateAndRecord(
            MultipartFile file,
            Complaint complaint,
            Assignment assignment,
            ComplaintImage complaintImage,
            BigDecimal targetLat,
            BigDecimal targetLon,
            BigDecimal deviceLat,
            BigDecimal deviceLon,
            OffsetDateTime deviceCapturedAt,
            String captureSource,
            String checkType,
            double toleranceMeters,
            User actor) {

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to read image bytes for verification: " + ex.getMessage());
        }

        // 1. SHA-256 Digest Computation
        String sha256 = computeSha256(fileBytes);

        // 2. Duplicate Detection
        boolean isDuplicate = false;
        List<ComplaintImage> existingWithSameHash = complaintImageRepository.findBySha256Hash(sha256);
        if (!existingWithSameHash.isEmpty()) {
            // Check if it's a duplicate of a previously uploaded distinct image
            for (ComplaintImage prev : existingWithSameHash) {
                if (complaintImage == null || complaintImage.getId() == null || !prev.getId().equals(complaintImage.getId())) {
                    isDuplicate = true;
                    break;
                }
            }
        }

        // 3. EXIF Extraction
        ExtractedImageMetadata exif = exifExtractor.extractMetadata(fileBytes);

        // 4. Coordinates & Distance Calculations
        // Effective coordinates: prefer device coordinates if provided, else fallback to EXIF GPS
        BigDecimal effectiveLat = deviceLat != null ? deviceLat : exif.exifLatitude();
        BigDecimal effectiveLon = deviceLon != null ? deviceLon : exif.exifLongitude();

        Double distToReport = distanceCalculator.calculateDistanceMeters(effectiveLat, effectiveLon, targetLat, targetLon);
        Double distDeviceToExif = null;
        if (deviceLat != null && exif.exifLatitude() != null) {
            distDeviceToExif = distanceCalculator.calculateDistanceMeters(deviceLat, deviceLon, exif.exifLatitude(), exif.exifLongitude());
        }

        // 5. Verification Flags Formulation
        List<String> flags = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        if (isDuplicate) {
            flags.add("DUPLICATE_IMAGE_DETECTED: SHA-256 matches previously uploaded photo (" + sha256.substring(0, 12) + "...)");
        } else {
            flags.add("UNIQUE_IMAGE_HASH: No duplicate file hash detected");
        }

        if (exif.hasExifGps()) {
            flags.add("EXIF_GPS_PRESENT: Camera embedded coordinates found (" + exif.exifLatitude() + ", " + exif.exifLongitude() + ")");
        } else {
            flags.add("EXIF_GPS_STRIPPED_OR_MISSING: Metadata stripped by device/browser; evaluated using device GPS");
        }

        boolean locationConsistent = false;
        if (distToReport != null) {
            if (distToReport <= toleranceMeters) {
                locationConsistent = true;
                flags.add("LOCATION_MATCHED: Distance " + distToReport + "m is within allowed tolerance of " + toleranceMeters + "m");
            } else {
                flags.add("LOCATION_MISMATCH: Photo distance " + distToReport + "m exceeds allowed tolerance of " + toleranceMeters + "m");
            }
        } else {
            flags.add("NO_GPS_COORDINATES: Neither device GPS nor EXIF coordinates were available");
        }

        if (distDeviceToExif != null) {
            if (distDeviceToExif > toleranceMeters * 2) {
                flags.add("COORDINATES_DISCREPANCY: Device GPS and EXIF GPS differ by " + distDeviceToExif + "m");
            }
        }

        // Timestamp Validation
        OffsetDateTime effectiveTimestamp = deviceCapturedAt != null ? deviceCapturedAt : exif.exifCapturedAt();
        if (effectiveTimestamp != null) {
            if (effectiveTimestamp.isAfter(now.plusMinutes(10))) {
                flags.add("SUSPICIOUS_FUTURE_TIMESTAMP: Capture time is in the future (" + effectiveTimestamp + ")");
            } else if (effectiveTimestamp.isBefore(now.minusHours(timeToleranceHours))) {
                long hoursAgo = Duration.between(effectiveTimestamp, now).toHours();
                flags.add("SUSPICIOUS_STALE_TIMESTAMP: Photo was captured " + hoursAgo + " hours ago (tolerance: " + timeToleranceHours + "h)");
            } else {
                flags.add("TIMESTAMP_VALID: Captured recently (" + effectiveTimestamp + ")");
            }
        } else {
            flags.add("TIMESTAMP_NOT_PROVIDED: No client capture timestamp recorded; using upload time");
        }

        if (exif.cameraModel() != null && !exif.cameraModel().isBlank()) {
            flags.add("DEVICE_MODEL: " + (exif.cameraMake() != null ? exif.cameraMake() + " " : "") + exif.cameraModel());
        }

        // 6. Formulate Overall Verification Status
        String status;
        double confidence = 0.90;

        if (isDuplicate) {
            status = "REJECTED";
            confidence = 0.0;
        } else if (!locationConsistent || (distToReport != null && distToReport > toleranceMeters)) {
            // Mismatched location -> Needs Review by Admin
            status = "NEEDS_REVIEW";
            confidence = 0.40;
        } else if (effectiveLat == null) {
            // No coordinates provided -> Needs Review
            status = "NEEDS_REVIEW";
            confidence = 0.50;
        } else if (effectiveTimestamp != null && (effectiveTimestamp.isAfter(now.plusMinutes(10)) || effectiveTimestamp.isBefore(now.minusHours(timeToleranceHours)))) {
            // Suspicious timestamp -> Needs Review
            status = "NEEDS_REVIEW";
            confidence = 0.60;
        } else {
            // Verified on site within tolerance
            status = "VERIFIED";
            confidence = exif.hasExifGps() ? 0.98 : 0.92;
        }

        String flagsJson;
        try {
            flagsJson = objectMapper.writeValueAsString(flags);
        } catch (JsonProcessingException e) {
            flagsJson = "[]";
        }

        // 7. Update ComplaintImage entity if provided
        if (complaintImage != null) {
            complaintImage.setSha256Hash(sha256);
            complaintImage.setDeviceLatitude(deviceLat);
            complaintImage.setDeviceLongitude(deviceLon);
            complaintImage.setDeviceCapturedAt(deviceCapturedAt);
            complaintImage.setExifLatitude(exif.exifLatitude());
            complaintImage.setExifLongitude(exif.exifLongitude());
            complaintImage.setExifCapturedAt(exif.exifCapturedAt());
            complaintImage.setExifCameraMake(exif.cameraMake());
            complaintImage.setExifCameraModel(exif.cameraModel());
            complaintImage.setDistanceToReportMeters(distToReport);
            complaintImage.setDistanceDeviceToExifMeters(distDeviceToExif);
            complaintImage.setVerificationStatus(status);
            complaintImage.setVerificationFlags(flagsJson);
            complaintImage.setCaptureSource(captureSource != null ? captureSource : "CAMERA");
            complaintImageRepository.save(complaintImage);
        }

        // 8. Record VerificationResult audit trail
        VerificationResult verifResult = new VerificationResult();
        verifResult.setComplaint(complaint);
        verifResult.setAssignment(assignment);
        verifResult.setImage(complaintImage);
        verifResult.setVerifierType("GPS_VERIFIER");
        verifResult.setVerifierUser(actor);
        verifResult.setVerificationStatus(status);
        verifResult.setCheckType(checkType);
        verifResult.setIsValidPlastic(!"REJECTED".equals(status));
        verifResult.setConfidenceScore(BigDecimal.valueOf(confidence));
        verifResult.setDeviceLatitude(deviceLat);
        verifResult.setDeviceLongitude(deviceLon);
        verifResult.setExifLatitude(exif.exifLatitude());
        verifResult.setExifLongitude(exif.exifLongitude());
        verifResult.setTargetLatitude(targetLat);
        verifResult.setTargetLongitude(targetLon);
        verifResult.setDistanceMeters(distToReport);
        verifResult.setDistanceToleranceMeters(toleranceMeters);
        verifResult.setCheckReasons(flagsJson);
        verifResult.setReviewDecision("VERIFIED".equals(status) ? "APPROVED" : "PENDING");
        verifResult.setAnalysisNotes(checkType + " verified with status: " + status + " (" + flags.size() + " audit checks)");

        verificationResultRepository.save(verifResult);

        return new PhotoVerificationResultDto(
                status,
                checkType,
                deviceLat,
                deviceLon,
                deviceCapturedAt,
                exif.exifLatitude(),
                exif.exifLongitude(),
                exif.exifCapturedAt(),
                exif.cameraMake(),
                exif.cameraModel(),
                sha256,
                distToReport,
                distDeviceToExif,
                toleranceMeters,
                flags,
                confidence,
                isDuplicate,
                locationConsistent
        );
    }

    @Transactional(readOnly = true)
    public List<VerificationResultResponse> getVerificationsForComplaint(UUID complaintId, UserPrincipal currentUser) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
        boolean isOwner = complaint.getCitizen() != null && complaint.getCitizen().getId().equals(currentUser.getId());
        boolean isAssignedVolunteer = false;
        if ("VOLUNTEER".equalsIgnoreCase(currentUser.getRole())) {
            List<Assignment> assignments = assignmentRepository.findByComplaintId(complaintId);
            for (Assignment a : assignments) {
                if (a.getVolunteer().getId().equals(currentUser.getId())) {
                    isAssignedVolunteer = true;
                    break;
                }
            }
        }

        if (!isAdmin && !isOwner && !isAssignedVolunteer) {
            throw new com.waste.management.exception.UnauthorizedActionException("Access Denied: You cannot view verification records for this complaint.");
        }

        return verificationResultRepository.findByComplaintIdOrderByVerifiedAtDesc(complaintId).stream()
                .map(VerificationResultResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VerificationResultResponse> getVerificationsForAssignment(UUID assignmentId, UserPrincipal currentUser) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
        boolean isVolunteer = assignment.getVolunteer().getId().equals(currentUser.getId());
        boolean isOwner = assignment.getComplaint().getCitizen() != null &&
                assignment.getComplaint().getCitizen().getId().equals(currentUser.getId());

        if (!isAdmin && !isVolunteer && !isOwner) {
            throw new com.waste.management.exception.UnauthorizedActionException("Access Denied: You cannot view verification records for this assignment.");
        }

        return verificationResultRepository.findByAssignmentId(assignmentId).stream()
                .map(VerificationResultResponse::fromEntity)
                .toList();
    }

    @Transactional
    public VerificationResultResponse processAdminDecision(
            UUID verificationId,
            VerificationDecisionRequest request,
            UserPrincipal adminUser) {
        VerificationResult result = verificationResultRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification result not found with id: " + verificationId));

        User admin = userRepository.findById(adminUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        result.setReviewDecision("APPROVE".equalsIgnoreCase(request.decision()) ? "APPROVED" : "REJECTED");
        result.setReviewedBy(admin);
        result.setReviewedAt(OffsetDateTime.now());
        result.setAdminNotes(request.reason());
        VerificationResult saved = verificationResultRepository.save(result);

        if ("CLEANUP_RESOLUTION".equalsIgnoreCase(result.getCheckType())) {
            Assignment assignment = result.getAssignment();
            if (assignment != null) {
                Complaint complaint = assignment.getComplaint();
                if ("APPROVE".equalsIgnoreCase(request.decision())) {
                    assignment.setStatus("COMPLETED");
                    assignment.setCompletedAt(OffsetDateTime.now());
                    assignment.setVerificationStatus("APPROVED_BY_ADMIN");
                    assignment.setVerificationNotes(request.reason());
                    assignmentRepository.save(assignment);

                    if (complaint != null) {
                        complaint.setStatus("RESOLVED");
                        complaintRepository.save(complaint);
                        progressUpdateRepository.save(new ProgressUpdate(
                                complaint,
                                admin,
                                "UNDER_REVIEW",
                                "RESOLVED",
                                "ADMIN_VERIFICATION_APPROVAL",
                                "Admin approved cleanup verification. Reason: " + request.reason()
                        ));
                        if (complaint.getCitizen() != null) {
                            notificationService.createNotification(
                                    complaint.getCitizen(),
                                    "Waste Report Resolved!",
                                    "Cleanup proof for '" + complaint.getTitle() + "' was verified and approved by admin.",
                                    "COMPLAINT_RESOLVED",
                                    complaint.getId()
                            );
                        }
                    }

                    volunteerProfileRepository.findByUserId(assignment.getVolunteer().getId()).ifPresent(p -> {
                        p.setTotalResolved(p.getTotalResolved() + 1);
                        volunteerProfileRepository.save(p);
                    });

                    notificationService.createNotification(
                            assignment.getVolunteer(),
                            "Cleanup Proof Approved",
                            "Your cleanup submission for '" + (complaint != null ? complaint.getTitle() : "Task") + "' was approved by admin.",
                            "CLEANUP_APPROVED",
                            assignment.getId()
                    );
                } else {
                    assignment.setStatus("IN_PROGRESS");
                    assignment.setVerificationStatus("REJECTED_BY_ADMIN");
                    assignment.setVerificationNotes(request.reason());
                    assignmentRepository.save(assignment);

                    if (complaint != null) {
                        complaint.setStatus("IN_PROGRESS");
                        complaintRepository.save(complaint);
                        progressUpdateRepository.save(new ProgressUpdate(
                                complaint,
                                admin,
                                "UNDER_REVIEW",
                                "IN_PROGRESS",
                                "ADMIN_VERIFICATION_REJECTION",
                                "Admin rejected cleanup proof. Reason: " + request.reason()
                        ));
                    }

                    notificationService.createNotification(
                            assignment.getVolunteer(),
                            "Cleanup Proof Rejected",
                            "Your cleanup proof was rejected by admin. Reason: " + request.reason() + ". Please re-submit valid on-site proof.",
                            "CLEANUP_REJECTED",
                            assignment.getId()
                    );
                }
            }
        } else if ("INITIAL_REPORT".equalsIgnoreCase(result.getCheckType())) {
            Complaint complaint = result.getComplaint();
            if (complaint != null) {
                if ("APPROVE".equalsIgnoreCase(request.decision())) {
                    complaint.setStatus("VERIFIED");
                    complaintRepository.save(complaint);
                    progressUpdateRepository.save(new ProgressUpdate(
                            complaint,
                            admin,
                            "UNDER_REVIEW",
                            "VERIFIED",
                            "ADMIN_VERIFICATION_APPROVAL",
                            "Admin approved flagged initial report. Reason: " + request.reason()
                    ));
                    if (complaint.getCitizen() != null) {
                        notificationService.createNotification(
                                complaint.getCitizen(),
                                "Report Verified by Admin",
                                "Your report '" + complaint.getTitle() + "' was reviewed and verified by admin.",
                                "COMPLAINT_VERIFIED",
                                complaint.getId()
                        );
                    }
                } else {
                    complaint.setStatus("REJECTED");
                    complaint.setRejectionReason(request.reason());
                    complaintRepository.save(complaint);
                    progressUpdateRepository.save(new ProgressUpdate(
                            complaint,
                            admin,
                            "UNDER_REVIEW",
                            "REJECTED",
                            "ADMIN_VERIFICATION_REJECTION",
                            "Admin rejected flagged report. Reason: " + request.reason()
                    ));
                    if (complaint.getCitizen() != null) {
                        notificationService.createNotification(
                                complaint.getCitizen(),
                                "Report Rejected by Admin",
                                "Your report '" + complaint.getTitle() + "' was rejected. Reason: " + request.reason(),
                                "COMPLAINT_REJECTED",
                                complaint.getId()
                        );
                    }
                }
            }
        }

        return VerificationResultResponse.fromEntity(saved);
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
