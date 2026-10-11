package com.waste.management.service;

import com.waste.management.dto.*;
import com.waste.management.entity.*;
import com.waste.management.repository.*;
import com.waste.management.security.JwtTokenProvider;
import com.waste.management.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PhotoVerificationServiceTests {

    @Autowired
    private PhotoVerificationService photoVerificationService;

    @Autowired
    private GeoDistanceCalculator distanceCalculator;

    @Autowired
    private ExifMetadataExtractor exifMetadataExtractor;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintImageRepository complaintImageRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private VerificationResultRepository verificationResultRepository;

    @Autowired
    private VolunteerProfileRepository volunteerProfileRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProgressUpdateRepository progressUpdateRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User citizen;
    private User volunteer;
    private User admin;
    private Complaint testComplaint;
    private Assignment testAssignment;

    // Standard valid JPEG byte array with JFIF header (no EXIF GPS)
    private final byte[] sampleJpegBytes = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
            0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
            (byte) 0xFF, (byte) 0xDB, 0x00, 0x43, 0x00, 0x08,
            0x06, 0x06, 0x07, 0x06, 0x05, 0x08, 0x07, 0x07,
            (byte) 0xFF, (byte) 0xD9
    };

    private final byte[] sampleJpegBytes2 = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
            0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
            (byte) 0xFF, (byte) 0xDB, 0x00, 0x43, 0x00, 0x09, // slightly different byte -> distinct hash
            0x06, 0x06, 0x07, 0x06, 0x05, 0x08, 0x07, 0x07,
            (byte) 0xFF, (byte) 0xD9
    };

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        assignmentRepository.deleteAll();
        progressUpdateRepository.deleteAll();
        verificationResultRepository.deleteAll();
        complaintImageRepository.deleteAll();
        complaintRepository.deleteAll();
        volunteerProfileRepository.deleteAll();
        userRepository.deleteAll();

        citizen = userRepository.save(new User("citizen_verif@test.org", passwordEncoder.encode("Pass123!"), "Verif Citizen", "+11111", "CITIZEN"));
        volunteer = userRepository.save(new User("vol_verif@test.org", passwordEncoder.encode("Pass123!"), "Verif Volunteer", "+22222", "VOLUNTEER"));
        admin = userRepository.save(new User("admin_verif@test.org", passwordEncoder.encode("Pass123!"), "Verif Admin", "+33333", "ADMIN"));

        volunteerProfileRepository.save(new VolunteerProfile(volunteer));

        testComplaint = new Complaint();
        testComplaint.setCitizen(citizen);
        testComplaint.setTitle("Park Litter Dump");
        testComplaint.setDescription("Plastic containers scattered in central park");
        testComplaint.setLatitude(new BigDecimal("12.971600"));
        testComplaint.setLongitude(new BigDecimal("77.594600"));
        testComplaint.setStatus("SUBMITTED");
        testComplaint = complaintRepository.save(testComplaint);

        testAssignment = new Assignment(testComplaint, volunteer, admin);
        testAssignment.setStatus("IN_PROGRESS");
        testAssignment = assignmentRepository.save(testAssignment);
    }

    @Test
    @DisplayName("1. Location Match: Photo coordinates within tolerance -> VERIFIED")
    void testLocationMatchWithinTolerance() {
        MockMultipartFile file = new MockMultipartFile("files", "matched.jpg", "image/jpeg", sampleJpegBytes);

        // Coordinates 30 meters away from complaint (well within 200m tolerance)
        BigDecimal deviceLat = new BigDecimal("12.971800");
        BigDecimal deviceLon = new BigDecimal("77.594700");
        OffsetDateTime capturedAt = OffsetDateTime.now().minusMinutes(5);

        ComplaintImage img = complaintImageRepository.save(
                new ComplaintImage(testComplaint, "https://res.cloudinary.com/demo/matched.jpg", "matched_id", "INITIAL_PROOF", citizen)
        );

        PhotoVerificationResultDto result = photoVerificationService.verifyInitialPhoto(
                file, testComplaint, img, deviceLat, deviceLon, capturedAt, "CAMERA", citizen
        );

        assertEquals("VERIFIED", result.verificationStatus(), "Status should be VERIFIED for on-site capture");
        assertTrue(result.distanceToTargetMeters() < 200.0, "Distance should be under 200 meters");
        assertTrue(result.isLocationConsistent(), "Location consistency flag should be true");
        assertFalse(result.isDuplicate(), "Should not be flagged as duplicate");
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("LOCATION_MATCHED")));

        // Verify ComplaintImage was persisted with verification details
        ComplaintImage updatedImg = complaintImageRepository.findById(img.getId()).orElseThrow();
        assertEquals("VERIFIED", updatedImg.getVerificationStatus());
        assertNotNull(updatedImg.getSha256Hash());
        assertNotNull(updatedImg.getDistanceToReportMeters());
    }

    @Test
    @DisplayName("2. Location Mismatch: Photo coordinates > tolerance -> NEEDS_REVIEW (Not deleted)")
    void testLocationMismatchOutsideTolerance() {
        MockMultipartFile file = new MockMultipartFile("files", "mismatch.jpg", "image/jpeg", sampleJpegBytes);

        // Coordinates 15 km away from complaint (> 200m tolerance)
        BigDecimal distantLat = new BigDecimal("13.100000");
        BigDecimal distantLon = new BigDecimal("77.594600");
        OffsetDateTime capturedAt = OffsetDateTime.now().minusMinutes(2);

        ComplaintImage img = complaintImageRepository.save(
                new ComplaintImage(testComplaint, "https://res.cloudinary.com/demo/mismatch.jpg", "mismatch_id", "INITIAL_PROOF", citizen)
        );

        PhotoVerificationResultDto result = photoVerificationService.verifyInitialPhoto(
                file, testComplaint, img, distantLat, distantLon, capturedAt, "CAMERA", citizen
        );

        assertEquals("NEEDS_REVIEW", result.verificationStatus(), "Status should be NEEDS_REVIEW for out-of-bounds photo");
        assertTrue(result.distanceToTargetMeters() > 200.0, "Distance should exceed 200 meters");
        assertFalse(result.isLocationConsistent(), "Location consistency should be false");
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("LOCATION_MISMATCH")));

        // Check audit record created in VerificationResult table
        List<VerificationResult> auditList = verificationResultRepository.findByComplaintIdOrderByVerifiedAtDesc(testComplaint.getId());
        assertFalse(auditList.isEmpty());
        assertEquals("NEEDS_REVIEW", auditList.get(0).getVerificationStatus());
        assertEquals("PENDING", auditList.get(0).getReviewDecision());
    }

    @Test
    @DisplayName("3. Missing EXIF: Device GPS valid without EXIF GPS -> VERIFIED (Missing EXIF != Fraud)")
    void testMissingExifWithValidDeviceGps() {
        // sampleJpegBytes has JFIF header without GPS EXIF tags
        MockMultipartFile file = new MockMultipartFile("files", "no_exif.jpg", "image/jpeg", sampleJpegBytes);

        BigDecimal deviceLat = new BigDecimal("12.971650");
        BigDecimal deviceLon = new BigDecimal("77.594620");
        OffsetDateTime capturedAt = OffsetDateTime.now();

        ComplaintImage img = complaintImageRepository.save(
                new ComplaintImage(testComplaint, "https://res.cloudinary.com/demo/no_exif.jpg", "no_exif_id", "INITIAL_PROOF", citizen)
        );

        PhotoVerificationResultDto result = photoVerificationService.verifyInitialPhoto(
                file, testComplaint, img, deviceLat, deviceLon, capturedAt, "CAMERA", citizen
        );

        // Crucial requirement: Missing EXIF metadata must NOT be treated as fraud
        assertEquals("VERIFIED", result.verificationStatus(), "Should be VERIFIED because device GPS matches site");
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("EXIF_GPS_STRIPPED_OR_MISSING")), "Flag for stripped EXIF should be present for transparency");
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("LOCATION_MATCHED")), "Location match flag should be recorded");
    }

    @Test
    @DisplayName("4. Duplicate Image: Same image SHA-256 hash -> REJECTED")
    void testDuplicateImageHashDetection() {
        MockMultipartFile file1 = new MockMultipartFile("files", "photo1.jpg", "image/jpeg", sampleJpegBytes);

        ComplaintImage img1 = complaintImageRepository.save(
                new ComplaintImage(testComplaint, "https://res.cloudinary.com/demo/p1.jpg", "p1_id", "INITIAL_PROOF", citizen)
        );
        photoVerificationService.verifyInitialPhoto(
                file1, testComplaint, img1, new BigDecimal("12.971600"), new BigDecimal("77.594600"), OffsetDateTime.now(), "CAMERA", citizen
        );

        // Upload second image with identical bytes
        MockMultipartFile duplicateFile = new MockMultipartFile("files", "duplicate.jpg", "image/jpeg", sampleJpegBytes);
        ComplaintImage img2 = complaintImageRepository.save(
                new ComplaintImage(testComplaint, "https://res.cloudinary.com/demo/p2.jpg", "p2_id", "INITIAL_PROOF", citizen)
        );

        PhotoVerificationResultDto duplicateResult = photoVerificationService.verifyInitialPhoto(
                duplicateFile, testComplaint, img2, new BigDecimal("12.971600"), new BigDecimal("77.594600"), OffsetDateTime.now(), "CAMERA", citizen
        );

        assertEquals("REJECTED", duplicateResult.verificationStatus(), "Duplicate hash should result in REJECTED status");
        assertTrue(duplicateResult.isDuplicate(), "isDuplicate flag should be true");
        assertTrue(duplicateResult.flags().stream().anyMatch(f -> f.contains("DUPLICATE_IMAGE_DETECTED")));
    }

    @Test
    @DisplayName("5. Volunteer Cleanup Proof Outside Distance -> NEEDS_REVIEW")
    void testVolunteerCleanupProofOutsideDistance() {
        MockMultipartFile proofFile = new MockMultipartFile("files", "cleanup.jpg", "image/jpeg", sampleJpegBytes2);

        // Volunteer submits proof from 2 km away
        BigDecimal distantVolunteerLat = new BigDecimal("12.990000");
        BigDecimal distantVolunteerLon = new BigDecimal("77.594600");

        ComplaintImage proofImg = complaintImageRepository.save(
                new ComplaintImage(testComplaint, "https://res.cloudinary.com/demo/cleanup.jpg", "cleanup_id", "RESOLUTION_PROOF", volunteer)
        );

        PhotoVerificationResultDto result = photoVerificationService.verifyCleanupPhoto(
                proofFile, testComplaint, testAssignment, proofImg,
                distantVolunteerLat, distantVolunteerLon, OffsetDateTime.now(), "CAMERA", volunteer
        );

        assertEquals("NEEDS_REVIEW", result.verificationStatus(), "Cleanup submitted far from report site requires review");
        assertEquals("CLEANUP_RESOLUTION", result.checkType());
        assertTrue(result.distanceToTargetMeters() > 200.0);
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("LOCATION_MISMATCH")));
    }

    @Test
    @DisplayName("6. Admin Decision: Admin approves flagged cleanup verification")
    void testAdminApprovalDecision() {
        MockMultipartFile proofFile = new MockMultipartFile("files", "cleanup_flagged.jpg", "image/jpeg", sampleJpegBytes2);

        ComplaintImage proofImg = complaintImageRepository.save(
                new ComplaintImage(testComplaint, "https://res.cloudinary.com/demo/cleanup_flagged.jpg", "cleanup_flagged_id", "RESOLUTION_PROOF", volunteer)
        );

        // Submitting with slight discrepancy triggering review
        photoVerificationService.verifyCleanupPhoto(
                proofFile, testComplaint, testAssignment, proofImg,
                new BigDecimal("12.975000"), new BigDecimal("77.594600"), OffsetDateTime.now(), "CAMERA", volunteer
        );

        List<VerificationResult> results = verificationResultRepository.findByAssignmentId(testAssignment.getId());
        assertFalse(results.isEmpty(), "VerificationResult record should be stored");
        VerificationResult audit = results.get(0);

        // Admin reviews and approves with reason
        VerificationDecisionRequest decisionReq = new VerificationDecisionRequest(
                "APPROVE",
                "Inspected photo: Landmarks in background confirm cleanup occurred at the park."
        );

        VerificationResultResponse decided = photoVerificationService.processAdminDecision(
                audit.getId(), decisionReq, UserPrincipal.create(admin)
        );

        assertEquals("APPROVED", decided.reviewDecision());
        assertEquals("Inspected photo: Landmarks in background confirm cleanup occurred at the park.", decided.adminNotes());

        // Check Assignment transitioned to COMPLETED
        Assignment updatedAssignment = assignmentRepository.findById(testAssignment.getId()).orElseThrow();
        assertEquals("COMPLETED", updatedAssignment.getStatus());
        assertEquals("APPROVED_BY_ADMIN", updatedAssignment.getVerificationStatus());
        assertNotNull(updatedAssignment.getCompletedAt());

        // Check Complaint transitioned to RESOLVED
        Complaint updatedComplaint = complaintRepository.findById(testComplaint.getId()).orElseThrow();
        assertEquals("RESOLVED", updatedComplaint.getStatus());
    }

    @Test
    @DisplayName("7. Admin Decision: Admin rejects flagged cleanup verification")
    void testAdminRejectionDecision() {
        MockMultipartFile proofFile = new MockMultipartFile("files", "cleanup_rejected.jpg", "image/jpeg", sampleJpegBytes2);

        ComplaintImage proofImg = complaintImageRepository.save(
                new ComplaintImage(testComplaint, "https://res.cloudinary.com/demo/cleanup_rejected.jpg", "cleanup_rejected_id", "RESOLUTION_PROOF", volunteer)
        );

        photoVerificationService.verifyCleanupPhoto(
                proofFile, testComplaint, testAssignment, proofImg,
                new BigDecimal("13.200000"), new BigDecimal("77.594600"), OffsetDateTime.now(), "CAMERA", volunteer
        );

        List<VerificationResult> results = verificationResultRepository.findByAssignmentId(testAssignment.getId());
        assertFalse(results.isEmpty());
        VerificationResult audit = results.get(0);

        VerificationDecisionRequest decisionReq = new VerificationDecisionRequest(
                "REJECT",
                "Photo does not match the park location and GPS is 20km off."
        );

        VerificationResultResponse decided = photoVerificationService.processAdminDecision(
                audit.getId(), decisionReq, UserPrincipal.create(admin)
        );

        assertEquals("REJECTED", decided.reviewDecision());

        // Check Assignment stays IN_PROGRESS so volunteer can re-clean / re-submit
        Assignment updatedAssignment = assignmentRepository.findById(testAssignment.getId()).orElseThrow();
        assertEquals("IN_PROGRESS", updatedAssignment.getStatus());
        assertEquals("REJECTED_BY_ADMIN", updatedAssignment.getVerificationStatus());
    }

    @Test
    @DisplayName("8. GeoDistanceCalculator: Accuracy of Haversine formula calculation")
    void testGeoDistanceCalculator() {
        // Distance between Vidhana Soudha (12.9797, 77.5907) and Cubbon Park Metro (12.9810, 77.5925) is ~240m
        BigDecimal lat1 = new BigDecimal("12.9797");
        BigDecimal lon1 = new BigDecimal("77.5907");
        BigDecimal lat2 = new BigDecimal("12.9810");
        BigDecimal lon2 = new BigDecimal("77.5925");

        Double distance = distanceCalculator.calculateDistanceMeters(lat1, lon1, lat2, lon2);
        assertNotNull(distance);
        assertTrue(distance > 200 && distance < 300, "Distance should be ~240m, got: " + distance);

        // Same point distance must be 0
        assertEquals(0.0, distanceCalculator.calculateDistanceMeters(lat1, lon1, lat1, lon1), 0.001);
    }
}
