package com.waste.management.service;

import com.waste.management.dto.*;
import com.waste.management.entity.*;
import com.waste.management.repository.*;
import com.waste.management.security.JwtTokenProvider;
import com.waste.management.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Test
    @DisplayName("9. EXIF GPS Available: Embedded coordinates match report site -> VERIFIED")
    void testExifGpsPresentAndMatched() {
        ExifMetadataExtractor mockExtractor = mock(ExifMetadataExtractor.class);
        when(mockExtractor.extractMetadata(any(byte[].class))).thenReturn(
                new ExtractedImageMetadata(
                        new BigDecimal("12.971620"),
                        new BigDecimal("77.594610"),
                        OffsetDateTime.now().minusMinutes(5),
                        "Sony",
                        "Alpha 7",
                        null,
                        null,
                        null,
                        true,
                        true,
                        false
                )
        );

        PhotoVerificationService customService = new PhotoVerificationService(
                mockExtractor, distanceCalculator, complaintImageRepository,
                verificationResultRepository, userRepository, assignmentRepository,
                complaintRepository, progressUpdateRepository, notificationService,
                volunteerProfileRepository, objectMapper
        );

        MockMultipartFile file = new MockMultipartFile("files", "with_exif.jpg", "image/jpeg", sampleJpegBytes);
        ComplaintImage img = complaintImageRepository.save(
                new ComplaintImage(testComplaint, "https://res.cloudinary.com/demo/exif.jpg", "exif_id", "INITIAL_PROOF", citizen)
        );

        // No device GPS provided; system relies entirely on valid EXIF GPS
        PhotoVerificationResultDto result = customService.verifyInitialPhoto(
                file, testComplaint, img, null, null, null, "CAMERA", citizen
        );

        assertEquals("VERIFIED", result.verificationStatus(), "Status should be VERIFIED based on EXIF GPS");
        assertEquals("EXIF_HEADER", result.coordinateSource());
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("EXIF_GPS_PRESENT")));
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("DEVICE_GPS_NOT_PROVIDED")));
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("LOCATION_MATCHED")));
        assertFalse(result.flags().stream().anyMatch(f -> f.contains("NO_GPS_COORDINATES")), "Should NOT claim NO_GPS_COORDINATES when EXIF GPS is present");
    }

    @Test
    @DisplayName("10. Both GPS Sources Absent: Neither device GPS nor EXIF coordinates available -> NEEDS_REVIEW (No false claims)")
    void testBothGpsSourcesAbsent() {
        MockMultipartFile file = new MockMultipartFile("files", "no_gps_anywhere.jpg", "image/jpeg", sampleJpegBytes);
        ComplaintImage img = complaintImageRepository.save(
                new ComplaintImage(testComplaint, "https://res.cloudinary.com/demo/no_gps.jpg", "no_gps_id", "INITIAL_PROOF", citizen)
        );

        PhotoVerificationResultDto result = photoVerificationService.verifyInitialPhoto(
                file, testComplaint, img, null, null, null, "FILE_UPLOAD", citizen
        );

        assertEquals("NEEDS_REVIEW", result.verificationStatus(), "Status should be NEEDS_REVIEW, not REJECTED");
        assertEquals("NONE", result.coordinateSource());
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("EXIF_GPS_NOT_FOUND")), "Must report EXIF_GPS_NOT_FOUND");
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("DEVICE_GPS_NOT_PROVIDED")), "Must report DEVICE_GPS_NOT_PROVIDED");
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("NO_GPS_COORDINATES")), "Must report NO_GPS_COORDINATES");
        assertFalse(result.flags().stream().anyMatch(f -> f.contains("evaluated using device GPS")), "Must NEVER claim evaluated using device GPS when device GPS is null");
        assertFalse(result.flags().stream().anyMatch(f -> f.contains("DUPLICATE_IMAGE_DETECTED")), "Missing GPS is not duplicate fraud");
    }

    @Test
    @DisplayName("11. Complaint Coordinates Separate from Photo GPS: Never copy complaint location into photo device GPS")
    void testComplaintCoordinatesSeparateFromPhotoGps() {
        MockMultipartFile file = new MockMultipartFile("files", "separate_coords.jpg", "image/jpeg", sampleJpegBytes);
        ComplaintImage img = complaintImageRepository.save(
                new ComplaintImage(testComplaint, "https://res.cloudinary.com/demo/separate.jpg", "separate_id", "INITIAL_PROOF", citizen)
        );

        // Complaint has valid target coordinates (12.9716, 77.5946), but photo has no device coordinates
        PhotoVerificationResultDto result = photoVerificationService.verifyInitialPhoto(
                file, testComplaint, img, null, null, null, "FILE_UPLOAD", citizen
        );

        assertNull(result.deviceLatitude(), "Photo device latitude must remain null, never fabricated from complaint");
        assertNull(result.deviceLongitude(), "Photo device longitude must remain null, never fabricated from complaint");
        assertEquals("NEEDS_REVIEW", result.verificationStatus());
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("NO_GPS_COORDINATES")));
    }

    @Test
    @DisplayName("12. File Modification Time Present without Genuine Capture Timestamp: Distinguishes file metadata from camera capture")
    void testFileLastModifiedWithoutGenuineCaptureTimestamp() {
        MockMultipartFile file = new MockMultipartFile("files", "file_mod.jpg", "image/jpeg", sampleJpegBytes);
        ComplaintImage img = complaintImageRepository.save(
                new ComplaintImage(testComplaint, "https://res.cloudinary.com/demo/file_mod.jpg", "mod_id", "INITIAL_PROOF", citizen)
        );

        OffsetDateTime fileModTime = OffsetDateTime.now().minusHours(3);

        PhotoVerificationResultDto result = photoVerificationService.verifyInitialPhoto(
                file, testComplaint, img,
                new BigDecimal("12.971610"), new BigDecimal("77.594610"),
                null, // deviceCapturedAt is explicitly null (uploaded file)
                "GALLERY_UPLOAD",
                15.0, // deviceAccuracy
                null, // deviceGpsTimestamp
                fileModTime, // fileLastModifiedAt
                citizen
        );

        assertTrue(result.flags().stream().anyMatch(f -> f.contains("FILE_MODIFICATION_TIME_RECORDED")), "Should record file modification timestamp");
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("TIMESTAMP_NOT_PROVIDED")), "Should indicate no genuine capture timestamp was recorded");
        assertFalse(result.flags().stream().anyMatch(f -> f.contains("using upload time as capture time")), "Should not claim upload time was original capture time");
    }

    @Test
    @DisplayName("13. Valid Captured Timestamp with Identified Source: Identifies DEVICE_CAMERA_CAPTURE")
    void testValidCapturedTimestampWithIdentifiedSource() {
        MockMultipartFile file = new MockMultipartFile("files", "captured.jpg", "image/jpeg", sampleJpegBytes);
        ComplaintImage img = complaintImageRepository.save(
                new ComplaintImage(testComplaint, "https://res.cloudinary.com/demo/captured.jpg", "cap_id", "INITIAL_PROOF", citizen)
        );

        OffsetDateTime liveCameraSnapTime = OffsetDateTime.now().minusMinutes(2);

        PhotoVerificationResultDto result = photoVerificationService.verifyInitialPhoto(
                file, testComplaint, img,
                new BigDecimal("12.971610"), new BigDecimal("77.594610"),
                liveCameraSnapTime,
                "IN_APP_CAMERA",
                5.0,
                liveCameraSnapTime,
                null,
                citizen
        );

        assertEquals("DEVICE_CAMERA_CAPTURE", result.timestampSource());
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("TIMESTAMP_VALID") && f.contains("DEVICE_CAMERA_CAPTURE")));
    }

    @Test
    @DisplayName("14. GPS Map Camera Filename without EXIF GPS: Flags VISIBLE_GPS_WATERMARK_NOT_MACHINE_VERIFIED")
    void testGpsMapCameraFilenameWithoutExifGps() {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "GPSMapCamera_20261011_153022.jpg",
                "image/jpeg",
                sampleJpegBytes
        );
        ComplaintImage img = complaintImageRepository.save(
                new ComplaintImage(testComplaint, "https://res.cloudinary.com/demo/watermark.jpg", "watermark_id", "INITIAL_PROOF", citizen)
        );

        PhotoVerificationResultDto result = photoVerificationService.verifyInitialPhoto(
                file, testComplaint, img, null, null, null, "GPS_MAP_CAMERA_FILE", citizen
        );

        assertEquals("NEEDS_REVIEW", result.verificationStatus(), "Should be flagged for Admin review, not rejected");
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("VISIBLE_GPS_WATERMARK_NOT_MACHINE_VERIFIED")), "Must flag visible watermark not machine verified");
        assertTrue(result.flags().stream().anyMatch(f -> f.contains("EXIF_GPS_NOT_FOUND")), "Must report EXIF_GPS_NOT_FOUND");
        assertFalse(result.flags().stream().anyMatch(f -> f.contains("evaluated using device GPS")), "Must not claim device GPS was evaluated");
    }

    @Test
    @DisplayName("15. Missing Metadata Results in Review Rather than Automatic Fraud Rejection")
    void testMissingMetadataResultsInReviewNotFraudRejection() {
        MockMultipartFile file = new MockMultipartFile("files", "stripped_web.jpg", "image/jpeg", sampleJpegBytes);
        ComplaintImage img = complaintImageRepository.save(
                new ComplaintImage(testComplaint, "https://res.cloudinary.com/demo/stripped.jpg", "stripped_id", "INITIAL_PROOF", citizen)
        );

        PhotoVerificationResultDto result = photoVerificationService.verifyInitialPhoto(
                file, testComplaint, img, null, null, null, "FILE_UPLOAD", citizen
        );

        assertEquals("NEEDS_REVIEW", result.verificationStatus(), "Missing metadata must result in review, not rejection");
        assertNotEquals("REJECTED", result.verificationStatus(), "Missing metadata must NEVER be automatically labeled REJECTED");

        List<VerificationResult> audits = verificationResultRepository.findByComplaintIdOrderByVerifiedAtDesc(testComplaint.getId());
        assertFalse(audits.isEmpty());
        assertEquals("NEEDS_REVIEW", audits.get(0).getVerificationStatus());
        assertEquals("PENDING", audits.get(0).getReviewDecision(), "Admin review must be PENDING");
    }
}
