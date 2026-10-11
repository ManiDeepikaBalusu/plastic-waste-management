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
import com.waste.management.dto.WatermarkOcrResult;
import com.waste.management.exception.ResourceNotFoundException;
import com.waste.management.exception.UnauthorizedActionException;
import com.waste.management.repository.*;
import com.waste.management.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private final WatermarkOcrService watermarkOcrService;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<UUID, Boolean> activeReevaluations = new ConcurrentHashMap<>();

    @Value("${app.verification.location-tolerance-meters:200.0}")
    private double locationToleranceMeters = 200.0;

    @Value("${app.verification.cleanup-tolerance-meters:200.0}")
    private double cleanupToleranceMeters = 200.0;

    @Value("${app.verification.time-tolerance-hours:24}")
    private long timeToleranceHours = 24;

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
            WatermarkOcrService watermarkOcrService,
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
        this.watermarkOcrService = watermarkOcrService;
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

        return verifyInitialPhoto(file, complaint, complaintImage, deviceLat, deviceLon, deviceCapturedAt, captureSource, null, null, null, null, null, citizen);
    }

    @Transactional
    public PhotoVerificationResultDto verifyInitialPhoto(
            MultipartFile file,
            Complaint complaint,
            ComplaintImage complaintImage,
            BigDecimal deviceLat,
            BigDecimal deviceLon,
            OffsetDateTime deviceCapturedAt,
            String captureSource,
            Double deviceAccuracy,
            OffsetDateTime deviceGpsTimestamp,
            OffsetDateTime fileLastModifiedAt,
            User citizen) {

        return verifyInitialPhoto(file, complaint, complaintImage, deviceLat, deviceLon, deviceCapturedAt, captureSource, deviceAccuracy, deviceGpsTimestamp, fileLastModifiedAt, null, null, citizen);
    }

    @Transactional
    public PhotoVerificationResultDto verifyInitialPhoto(
            MultipartFile file,
            Complaint complaint,
            ComplaintImage complaintImage,
            BigDecimal deviceLat,
            BigDecimal deviceLon,
            OffsetDateTime deviceCapturedAt,
            String captureSource,
            Double deviceAccuracy,
            OffsetDateTime deviceGpsTimestamp,
            OffsetDateTime fileLastModifiedAt,
            BigDecimal browserUploadLat,
            BigDecimal browserUploadLon,
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
                deviceAccuracy,
                deviceGpsTimestamp,
                fileLastModifiedAt,
                browserUploadLat,
                browserUploadLon,
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

        return verifyCleanupPhoto(file, complaint, assignment, complaintImage, deviceLat, deviceLon, deviceCapturedAt, captureSource, null, null, null, null, null, volunteer);
    }

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
            Double deviceAccuracy,
            OffsetDateTime deviceGpsTimestamp,
            OffsetDateTime fileLastModifiedAt,
            User volunteer) {

        return verifyCleanupPhoto(file, complaint, assignment, complaintImage, deviceLat, deviceLon, deviceCapturedAt, captureSource, deviceAccuracy, deviceGpsTimestamp, fileLastModifiedAt, null, null, volunteer);
    }

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
            Double deviceAccuracy,
            OffsetDateTime deviceGpsTimestamp,
            OffsetDateTime fileLastModifiedAt,
            BigDecimal browserUploadLat,
            BigDecimal browserUploadLon,
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
                deviceAccuracy,
                deviceGpsTimestamp,
                fileLastModifiedAt,
                browserUploadLat,
                browserUploadLon,
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
            Double deviceAccuracy,
            OffsetDateTime deviceGpsTimestamp,
            OffsetDateTime fileLastModifiedAt,
            BigDecimal browserUploadLat,
            BigDecimal browserUploadLon,
            String checkType,
            double toleranceMeters,
            User actor) {

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to read image bytes for verification: " + ex.getMessage());
        }

        String safeFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";

        // 1. SHA-256 Digest Computation
        String sha256 = computeSha256(fileBytes);

        // 2. Duplicate Detection
        boolean isDuplicate = false;
        List<ComplaintImage> existingWithSameHash = complaintImageRepository.findBySha256Hash(sha256);
        if (!existingWithSameHash.isEmpty()) {
            for (ComplaintImage prev : existingWithSameHash) {
                if (complaintImage == null || complaintImage.getId() == null || !prev.getId().equals(complaintImage.getId())) {
                    isDuplicate = true;
                    break;
                }
            }
        }

        // 3. Strict Coordinate Source Resolution
        // NEVER treat browser upload GPS as live photo capture location for gallery/file uploads.
        boolean isLiveCamera = "IN_APP_CAMERA".equalsIgnoreCase(captureSource) || "CAMERA".equalsIgnoreCase(captureSource);

        BigDecimal actualDeviceLat = isLiveCamera ? deviceLat : null;
        BigDecimal actualDeviceLon = isLiveCamera ? deviceLon : null;
        OffsetDateTime actualDeviceCapturedAt = isLiveCamera ? deviceCapturedAt : null;

        BigDecimal actualBrowserUploadLat = browserUploadLat != null ? browserUploadLat : (!isLiveCamera ? deviceLat : null);
        BigDecimal actualBrowserUploadLon = browserUploadLon != null ? browserUploadLon : (!isLiveCamera ? deviceLon : null);

        // 4. EXIF Extraction
        ExtractedImageMetadata exif = exifExtractor.extractMetadata(fileBytes);

        // 5. Watermark OCR Extraction
        WatermarkOcrResult ocrResult = watermarkOcrService.extractWatermark(fileBytes, safeFilename);

        // 6. Coordinates & Source Selection Policy
        boolean hasLiveDeviceInput = (actualDeviceLat != null && actualDeviceLon != null);
        boolean hasLiveDeviceGps = hasLiveDeviceInput && distanceCalculator.isValidCoordinate(actualDeviceLat, actualDeviceLon);
        boolean hasExifGps = exif.hasExifGps() && distanceCalculator.isValidCoordinate(exif.exifLatitude(), exif.exifLongitude());
        boolean hasOcrGps = ocrResult.watermarkDetected() && ocrResult.ocrLatitude() != null && ocrResult.ocrLongitude() != null && distanceCalculator.isValidCoordinate(ocrResult.ocrLatitude(), ocrResult.ocrLongitude());

        BigDecimal effectiveLat;
        BigDecimal effectiveLon;
        String coordinateSource;

        if (hasLiveDeviceInput) {
            effectiveLat = actualDeviceLat;
            effectiveLon = actualDeviceLon;
            coordinateSource = hasExifGps ? "DEVICE_AND_EXIF_GPS" : "DEVICE_CAMERA_GPS";
        } else if (hasExifGps) {
            effectiveLat = exif.exifLatitude();
            effectiveLon = exif.exifLongitude();
            coordinateSource = "EXIF_HEADER";
        } else if (hasOcrGps) {
            effectiveLat = ocrResult.ocrLatitude();
            effectiveLon = ocrResult.ocrLongitude();
            coordinateSource = "OCR_WATERMARK_UNVERIFIED";
        } else {
            effectiveLat = null;
            effectiveLon = null;
            coordinateSource = "NONE";
        }

        // 7. Distance Calculations
        Double distToReport = null;
        if (effectiveLat != null && effectiveLon != null && targetLat != null && targetLon != null) {
            if (distanceCalculator.isValidCoordinate(effectiveLat, effectiveLon) && distanceCalculator.isValidCoordinate(targetLat, targetLon)) {
                distToReport = distanceCalculator.calculateDistanceMeters(effectiveLat, effectiveLon, targetLat, targetLon);
            }
        }

        Double distDeviceToExif = null;
        if (hasLiveDeviceGps && hasExifGps) {
            distDeviceToExif = distanceCalculator.calculateDistanceMeters(actualDeviceLat, actualDeviceLon, exif.exifLatitude(), exif.exifLongitude());
        }

        // 8. Verification Flags Formulation
        List<String> flags = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        // 8.1 Duplicate Check
        if (isDuplicate) {
            flags.add("DUPLICATE_IMAGE_DETECTED: SHA-256 matches previously uploaded photo (" + sha256.substring(0, 12) + "...)");
        } else {
            flags.add("UNIQUE_IMAGE_HASH: No duplicate file hash detected");
        }

        // 8.2 EXIF GPS Check
        if (hasExifGps) {
            flags.add("EXIF_GPS_PRESENT: Camera embedded coordinates found (" + exif.exifLatitude() + ", " + exif.exifLongitude() + ")");
        } else {
            if (hasLiveDeviceGps) {
                flags.add("EXIF_GPS_STRIPPED_OR_MISSING: Metadata stripped or absent; evaluated using device camera GPS");
            } else {
                flags.add("EXIF_GPS_NOT_FOUND: No embedded EXIF GPS coordinates found in image metadata");
            }
        }

        // 8.3 Device GPS Check
        if (hasLiveDeviceInput) {
            String accStr = deviceAccuracy != null ? ", accuracy: ±" + deviceAccuracy + "m" : "";
            flags.add("DEVICE_GPS_PRESENT: Device camera GPS coordinates recorded (" + actualDeviceLat + ", " + actualDeviceLon + accStr + ")");
        } else if (isLiveCamera) {
            flags.add("DEVICE_GPS_NOT_PROVIDED: In-app camera used but no device GPS coordinates recorded");
        } else {
            flags.add("DEVICE_GPS_NOT_PROVIDED: No device GPS coordinates recorded by client");
        }

        // 8.4 Browser Upload Location Note (Strict separation)
        if (actualBrowserUploadLat != null && actualBrowserUploadLon != null) {
            flags.add("BROWSER_UPLOAD_LOCATION_RECORDED: Browser location at upload time was (" + actualBrowserUploadLat + ", " + actualBrowserUploadLon + ") - not used as photo capture location for file uploads");
        }

        // 8.5 OCR Watermark Evaluation
        boolean indicatesWatermarkApp = (safeFilename != null && safeFilename.toLowerCase().contains("gpsmapcamera"))
                || "GPS_MAP_CAMERA_FILE".equalsIgnoreCase(captureSource);

        if (ocrResult.watermarkDetected()) {
            flags.add("OCR_WATERMARK_DETECTED: Visible watermark text detected in photo footer");
            if (hasOcrGps) {
                flags.add("OCR_WATERMARK_COORDINATES: Extracted watermark coordinates (" + ocrResult.ocrLatitude() + ", " + ocrResult.ocrLongitude() + ") with OCR confidence " + String.format(Locale.ROOT, "%.2f", ocrResult.confidence()));
            } else {
                flags.add("VISIBLE_GPS_WATERMARK_COORDINATES_UNREADABLE: Watermark text detected but valid coordinates could not be extracted");
            }
        } else if ("AI_SERVICE_UNAVAILABLE".equalsIgnoreCase(ocrResult.ocrStatus())) {
            flags.add("OCR_SERVICE_UNAVAILABLE: AI OCR service unavailable or timed out; manual admin review required");
        } else if ("OCR_FAILED".equalsIgnoreCase(ocrResult.ocrStatus())) {
            flags.add("OCR_PARSING_FAILED: AI OCR engine reported parsing error on image");
        }

        if ((indicatesWatermarkApp || ocrResult.watermarkDetected()) && !hasExifGps) {
            flags.add("VISIBLE_GPS_WATERMARK_NOT_MACHINE_VERIFIED: Photo indicates visible GPS watermark or GPS Map Camera app, but coordinates were not verified from camera EXIF");
        }

        // 8.6 Coordinate Validation & Location Proximity Check
        boolean locationConsistent = false;
        boolean hasInvalidCoordinates = false;

        // Swapped coordinate detection
        if (targetLat != null && targetLon != null && distanceCalculator.areCoordinatesSwapped(targetLat, targetLon)) {
            flags.add("COORDINATES_SWAPPED_DETECTED: Target latitude (" + targetLat + ") exceeds 90 degrees; coordinates appear inverted");
            hasInvalidCoordinates = true;
        }
        if (effectiveLat != null && effectiveLon != null && distanceCalculator.areCoordinatesSwapped(effectiveLat, effectiveLon)) {
            flags.add("COORDINATES_SWAPPED_DETECTED: Photo latitude (" + effectiveLat + ") exceeds 90 degrees; coordinates appear inverted");
            hasInvalidCoordinates = true;
        }

        // Range boundary validation
        if ((targetLat != null && targetLon != null && !distanceCalculator.isValidCoordinate(targetLat, targetLon)) ||
            (effectiveLat != null && effectiveLon != null && !distanceCalculator.isValidCoordinate(effectiveLat, effectiveLon))) {
            flags.add("INVALID_COORDINATES: Coordinates outside valid geographic range (lat: -90..90, lon: -180..180)");
            hasInvalidCoordinates = true;
        }

        if (effectiveLat != null && effectiveLon != null) {
            if (targetLat != null && targetLon != null) {
                if (hasInvalidCoordinates || distToReport == null) {
                    flags.add("CALCULATION_UNAVAILABLE: Real distance could not be computed due to invalid coordinate bounds");
                } else if ("OCR_WATERMARK_UNVERIFIED".equals(coordinateSource)) {
                    if (distToReport <= toleranceMeters) {
                        locationConsistent = true;
                        flags.add("OCR_WATERMARK_LOCATION_MATCHED: Watermark coordinates (" + effectiveLat + ", " + effectiveLon + ") are " + String.format(Locale.ROOT, "%.1f", distToReport) + "m from report site (unverified text overlay)");
                        flags.add("OCR_WATERMARK_REQUIRES_ADMIN_VERIFICATION: Watermark coordinates matched within tolerance, but OCR watermark data is unverified and cannot establish authenticity; flagged for Admin review");
                    } else {
                        flags.add("OCR_WATERMARK_LOCATION_MISMATCH: Watermark coordinates (" + effectiveLat + ", " + effectiveLon + ") are " + String.format(Locale.ROOT, "%.1f", distToReport) + "m from report site (exceeds " + toleranceMeters + "m tolerance)");
                    }
                } else if (distToReport <= toleranceMeters) {
                    locationConsistent = true;
                    flags.add("LOCATION_MATCHED: Distance " + String.format(Locale.ROOT, "%.1f", distToReport) + "m is within allowed tolerance of " + toleranceMeters + "m");
                } else {
                    flags.add("LOCATION_MISMATCH: Photo distance " + String.format(Locale.ROOT, "%.1f", distToReport) + "m exceeds allowed tolerance of " + toleranceMeters + "m");
                }
            } else {
                flags.add("TARGET_COORDINATES_MISSING: Report location missing for distance calculation");
            }
        } else {
            flags.add("NO_GPS_COORDINATES: Neither device camera GPS, EXIF GPS, nor readable watermark coordinates were available");
        }

        if (distDeviceToExif != null && distDeviceToExif > toleranceMeters * 2) {
            flags.add("COORDINATES_DISCREPANCY: Device GPS and EXIF GPS differ by " + String.format(Locale.ROOT, "%.1f", distDeviceToExif) + "m");
        }

        // 8.7 Timestamp Handling & Validation
        // Preserve raw OCR timestamp text without destructive timezone alterations.
        String timestampSource;
        OffsetDateTime effectiveTimestamp;
        if (actualDeviceCapturedAt != null) {
            effectiveTimestamp = actualDeviceCapturedAt;
            timestampSource = "DEVICE_CAMERA_CAPTURE";
        } else if (exif.exifCapturedAt() != null) {
            effectiveTimestamp = exif.exifCapturedAt();
            timestampSource = "EXIF_HEADER";
        } else if (ocrResult.ocrTimestamp() != null && !ocrResult.ocrTimestamp().isBlank()) {
            effectiveTimestamp = null;
            timestampSource = "OCR_WATERMARK_TIMESTAMP_UNVERIFIED";
        } else {
            effectiveTimestamp = null;
            timestampSource = "UPLOAD_TIME_FALLBACK";
        }

        if (ocrResult.ocrTimestamp() != null && !ocrResult.ocrTimestamp().isBlank()) {
            flags.add("OCR_WATERMARK_TIMESTAMP_DETECTED: Visible watermark timestamp '" + ocrResult.ocrTimestamp() + "' (unverified overlay)");
        }

        if ("OCR_WATERMARK_TIMESTAMP_UNVERIFIED".equals(timestampSource)) {
            flags.add("TIMESTAMP_FROM_WATERMARK: Timestamp extracted from visible watermark; requires admin verification");
        } else if (effectiveTimestamp != null) {
            if (effectiveTimestamp.isAfter(now.plusMinutes(10))) {
                flags.add("SUSPICIOUS_FUTURE_TIMESTAMP: Capture time is in the future (" + effectiveTimestamp + ", source: " + timestampSource + ")");
            } else if (effectiveTimestamp.isBefore(now.minusHours(timeToleranceHours))) {
                long hoursAgo = Duration.between(effectiveTimestamp, now).toHours();
                flags.add("SUSPICIOUS_STALE_TIMESTAMP: Photo was captured " + hoursAgo + " hours ago (tolerance: " + timeToleranceHours + "h, source: " + timestampSource + ")");
            } else {
                flags.add("TIMESTAMP_VALID: Captured recently at " + effectiveTimestamp + " (source: " + timestampSource + ")");
            }
        } else {
            flags.add("TIMESTAMP_NOT_PROVIDED: No client or EXIF capture timestamp recorded; upload time recorded as " + now);
        }

        if (fileLastModifiedAt != null) {
            flags.add("FILE_MODIFICATION_TIME_RECORDED: File last modified at " + fileLastModifiedAt + " (filesystem attribute, not verified as capture time)");
        }

        if (exif.cameraModel() != null && !exif.cameraModel().isBlank()) {
            flags.add("DEVICE_MODEL: " + (exif.cameraMake() != null ? exif.cameraMake() + " " : "") + exif.cameraModel());
        }

        // 9. Overall Verification Status Formulation
        // CRITICAL: OCR confidence alone must NEVER establish authenticity.
        // Even when distance is ~0.0m, status MUST remain NEEDS_REVIEW.
        String status;
        double confidence;

        if (isDuplicate) {
            status = "REJECTED";
            confidence = 0.0;
        } else if ("OCR_WATERMARK_UNVERIFIED".equals(coordinateSource)) {
            status = "NEEDS_REVIEW";
            confidence = Math.min(0.65, ocrResult.confidence());
        } else if (effectiveLat == null || targetLat == null) {
            status = "NEEDS_REVIEW";
            confidence = 0.50;
        } else if (hasInvalidCoordinates || distToReport == null) {
            status = "NEEDS_REVIEW";
            confidence = 0.30;
        } else if (!locationConsistent || distToReport > toleranceMeters) {
            status = "NEEDS_REVIEW";
            confidence = 0.40;
        } else if (effectiveTimestamp != null && (effectiveTimestamp.isAfter(now.plusMinutes(10)) || effectiveTimestamp.isBefore(now.minusHours(timeToleranceHours)))) {
            status = "NEEDS_REVIEW";
            confidence = 0.60;
        } else {
            status = "VERIFIED";
            confidence = hasExifGps ? 0.98 : 0.92;
        }

        // Safe Diagnostic Logging
        logger.info("[VERIFICATION_COORDS] File: '{}', CheckType: {}, Source: {}, Target: ({}, {}), Candidate: ({}, {}), Dist: {}m, BrowserUpload: ({}, {}), Status: {}",
                safeFilename, checkType, coordinateSource, targetLat, targetLon, effectiveLat, effectiveLon,
                distToReport != null ? String.format(Locale.ROOT, "%.1f", distToReport) : "null",
                actualBrowserUploadLat, actualBrowserUploadLon, status);

        String flagsJson;
        try {
            flagsJson = objectMapper.writeValueAsString(flags);
        } catch (JsonProcessingException e) {
            flagsJson = "[]";
        }

        // 10. Update ComplaintImage Entity
        if (complaintImage != null) {
            complaintImage.setSha256Hash(sha256);
            complaintImage.setDeviceLatitude(actualDeviceLat);
            complaintImage.setDeviceLongitude(actualDeviceLon);
            complaintImage.setDeviceCapturedAt(actualDeviceCapturedAt);
            complaintImage.setExifLatitude(exif.exifLatitude());
            complaintImage.setExifLongitude(exif.exifLongitude());
            complaintImage.setExifCapturedAt(exif.exifCapturedAt());
            complaintImage.setExifCameraMake(exif.cameraMake());
            complaintImage.setExifCameraModel(exif.cameraModel());
            complaintImage.setDistanceToReportMeters(distToReport);
            complaintImage.setDistanceDeviceToExifMeters(distDeviceToExif);
            complaintImage.setVerificationStatus(status);
            complaintImage.setVerificationFlags(flagsJson);
            complaintImage.setCaptureSource(captureSource != null ? captureSource : "GALLERY_UPLOAD");
            complaintImage.setFileLastModifiedAt(fileLastModifiedAt);
            complaintImage.setDeviceGpsAccuracy(deviceAccuracy);
            complaintImage.setDeviceGpsTimestamp(deviceGpsTimestamp);
            complaintImage.setCoordinateSource(coordinateSource);
            complaintImage.setTimestampSource(timestampSource);
            complaintImage.setOcrWatermarkText(ocrResult.ocrRawText());
            complaintImage.setOcrWatermarkStatus(ocrResult.ocrStatus());
            complaintImage.setOcrLatitude(ocrResult.ocrLatitude());
            complaintImage.setOcrLongitude(ocrResult.ocrLongitude());
            complaintImage.setOcrTimestamp(ocrResult.ocrTimestamp());
            complaintImage.setBrowserUploadLatitude(actualBrowserUploadLat);
            complaintImage.setBrowserUploadLongitude(actualBrowserUploadLon);
            complaintImageRepository.save(complaintImage);
        }

        // 11. Record VerificationResult Entity
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
        verifResult.setDeviceLatitude(actualDeviceLat);
        verifResult.setDeviceLongitude(actualDeviceLon);
        verifResult.setExifLatitude(exif.exifLatitude());
        verifResult.setExifLongitude(exif.exifLongitude());
        verifResult.setTargetLatitude(targetLat);
        verifResult.setTargetLongitude(targetLon);
        verifResult.setDistanceMeters(distToReport);
        verifResult.setDistanceToleranceMeters(toleranceMeters);
        verifResult.setCheckReasons(flagsJson);
        verifResult.setReviewDecision("VERIFIED".equals(status) ? "APPROVED" : "PENDING");
        verifResult.setAnalysisNotes(checkType + " verified with status: " + status + " (" + flags.size() + " audit checks)");
        verifResult.setCoordinateSource(coordinateSource);
        verifResult.setTimestampSource(timestampSource);
        verifResult.setDeviceGpsAccuracy(deviceAccuracy);
        verifResult.setOcrWatermarkText(ocrResult.ocrRawText());
        verifResult.setOcrWatermarkStatus(ocrResult.ocrStatus());
        verifResult.setOcrLatitude(ocrResult.ocrLatitude());
        verifResult.setOcrLongitude(ocrResult.ocrLongitude());
        verifResult.setOcrTimestamp(ocrResult.ocrTimestamp());
        verifResult.setBrowserUploadLatitude(actualBrowserUploadLat);
        verifResult.setBrowserUploadLongitude(actualBrowserUploadLon);

        verificationResultRepository.save(verifResult);

        return new PhotoVerificationResultDto(
                status,
                checkType,
                actualDeviceLat,
                actualDeviceLon,
                actualDeviceCapturedAt,
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
                locationConsistent,
                coordinateSource,
                timestampSource,
                deviceAccuracy,
                ocrResult.ocrRawText(),
                ocrResult.ocrStatus(),
                ocrResult.ocrLatitude(),
                ocrResult.ocrLongitude(),
                ocrResult.ocrTimestamp(),
                actualBrowserUploadLat,
                actualBrowserUploadLon
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

        if ("APPROVED".equalsIgnoreCase(result.getReviewDecision()) || "REJECTED".equalsIgnoreCase(result.getReviewDecision())) {
            throw new IllegalStateException("Verification record " + verificationId + " has already been reviewed with decision: " + result.getReviewDecision());
        }

        String decisionUpper = "APPROVE".equalsIgnoreCase(request.decision()) ? "APPROVED" : "REJECTED";
        String verifStatusUpper = "APPROVE".equalsIgnoreCase(request.decision()) ? "APPROVED_BY_ADMIN" : "REJECTED_BY_ADMIN";

        result.setReviewDecision(decisionUpper);
        result.setVerificationStatus(verifStatusUpper);
        result.setReviewedBy(admin);
        result.setReviewedAt(OffsetDateTime.now());
        result.setAdminNotes(request.reason());

        if (result.getImage() != null) {
            result.getImage().setVerificationStatus(verifStatusUpper);
            complaintImageRepository.save(result.getImage());
        }

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

    @Transactional
    public VerificationResultResponse reEvaluateVerification(UUID verificationId, UserPrincipal adminUser) {
        if (adminUser == null || !"ADMIN".equalsIgnoreCase(adminUser.getRole())) {
            throw new UnauthorizedActionException("Only administrators can re-evaluate verification evidence.");
        }

        if (activeReevaluations.putIfAbsent(verificationId, Boolean.TRUE) != null) {
            throw new IllegalStateException("Re-evaluation already in progress for verification ID: " + verificationId);
        }

        try {
            VerificationResult result = verificationResultRepository.findById(verificationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Verification result not found with id: " + verificationId));

            Complaint complaint = result.getComplaint();
            ComplaintImage image = result.getImage();
            if (image == null || image.getImageUrl() == null || image.getImageUrl().isBlank()) {
                throw new IllegalStateException("Verification record has no associated image URL to re-evaluate.");
            }

            User adminEntity = userRepository.findById(adminUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

            // Download image bytes
            byte[] imageBytes = downloadImageBytes(image.getImageUrl());

            // Run OCR extraction
            String filename = "verification_" + verificationId + ".jpg";
            WatermarkOcrResult ocrResult = watermarkOcrService.extractWatermark(imageBytes, filename);

            // Recompute evaluation against complaint target coordinates
            BigDecimal targetLat = result.getTargetLatitude() != null ? result.getTargetLatitude() : (complaint != null ? complaint.getLatitude() : null);
            BigDecimal targetLon = result.getTargetLongitude() != null ? result.getTargetLongitude() : (complaint != null ? complaint.getLongitude() : null);

            boolean hasExifGps = result.getExifLatitude() != null && result.getExifLongitude() != null && distanceCalculator.isValidCoordinate(result.getExifLatitude(), result.getExifLongitude());
            boolean hasLiveDeviceGps = "DEVICE_CAMERA_GPS".equalsIgnoreCase(result.getCoordinateSource()) && result.getDeviceLatitude() != null && result.getDeviceLongitude() != null;
            boolean hasOcrGps = ocrResult.watermarkDetected() && ocrResult.ocrLatitude() != null && ocrResult.ocrLongitude() != null && distanceCalculator.isValidCoordinate(ocrResult.ocrLatitude(), ocrResult.ocrLongitude());

            BigDecimal effectiveLat;
            BigDecimal effectiveLon;
            String coordinateSource;

            if (hasLiveDeviceGps) {
                effectiveLat = result.getDeviceLatitude();
                effectiveLon = result.getDeviceLongitude();
                coordinateSource = "DEVICE_CAMERA_GPS";
            } else if (hasExifGps) {
                effectiveLat = result.getExifLatitude();
                effectiveLon = result.getExifLongitude();
                coordinateSource = "EXIF_HEADER";
            } else if (hasOcrGps) {
                effectiveLat = ocrResult.ocrLatitude();
                effectiveLon = ocrResult.ocrLongitude();
                coordinateSource = "OCR_WATERMARK_UNVERIFIED";
            } else {
                effectiveLat = null;
                effectiveLon = null;
                coordinateSource = "NONE";
            }

            Double distToReport = null;
            if (effectiveLat != null && effectiveLon != null && targetLat != null && targetLon != null) {
                if (distanceCalculator.isValidCoordinate(effectiveLat, effectiveLon) && distanceCalculator.isValidCoordinate(targetLat, targetLon)) {
                    distToReport = distanceCalculator.calculateDistanceMeters(effectiveLat, effectiveLon, targetLat, targetLon);
                }
            }

            List<String> flags = new ArrayList<>();
            flags.add("UNIQUE_IMAGE_HASH: Re-evaluated verification record");

            if (hasExifGps) {
                flags.add("EXIF_GPS_PRESENT: Camera embedded coordinates found (" + result.getExifLatitude() + ", " + result.getExifLongitude() + ")");
            } else {
                flags.add("EXIF_GPS_NOT_FOUND: No embedded EXIF GPS coordinates found in image metadata");
            }

            double tolerance = result.getDistanceToleranceMeters() != null ? result.getDistanceToleranceMeters() : locationToleranceMeters;

            if ("OCR_WATERMARK_UNVERIFIED".equals(coordinateSource)) {
                if (distToReport != null && distToReport <= tolerance) {
                    flags.add("OCR_WATERMARK_LOCATION_MATCHED: Watermark coordinates (" + effectiveLat + ", " + effectiveLon + ") are " + String.format(Locale.ROOT, "%.1f", distToReport) + "m from report site (unverified text overlay)");
                    flags.add("OCR_WATERMARK_REQUIRES_ADMIN_VERIFICATION: Watermark coordinates matched within tolerance, but OCR watermark data is unverified and cannot establish authenticity; flagged for Admin review");
                } else if (distToReport != null) {
                    flags.add("OCR_WATERMARK_LOCATION_MISMATCH: Watermark coordinates (" + effectiveLat + ", " + effectiveLon + ") are " + String.format(Locale.ROOT, "%.1f", distToReport) + "m from report site (exceeds " + tolerance + "m tolerance)");
                }
            } else if (distToReport != null && distToReport <= tolerance) {
                flags.add("LOCATION_MATCHED: Distance " + String.format(Locale.ROOT, "%.1f", distToReport) + "m is within allowed tolerance");
            } else if (distToReport != null) {
                flags.add("LOCATION_MISMATCH: Photo distance " + String.format(Locale.ROOT, "%.1f", distToReport) + "m exceeds allowed tolerance");
            } else {
                flags.add("NO_GPS_COORDINATES: Neither device GPS nor EXIF coordinates were available");
            }

            if (result.getBrowserUploadLatitude() != null && result.getBrowserUploadLongitude() != null) {
                flags.add("BROWSER_UPLOAD_LOCATION_RECORDED: Browser location at upload time was (" + result.getBrowserUploadLatitude() + ", " + result.getBrowserUploadLongitude() + ") - not used as photo capture location for file uploads");
            }

            String timestampSource = result.getTimestampSource();
            if (ocrResult.ocrTimestamp() != null && !ocrResult.ocrTimestamp().isBlank()) {
                flags.add("OCR_WATERMARK_TIMESTAMP_DETECTED: Visible watermark timestamp '" + ocrResult.ocrTimestamp() + "' (unverified overlay)");
                if (result.getExifLatitude() == null && result.getDeviceLatitude() == null) {
                    timestampSource = "OCR_WATERMARK_TIMESTAMP_UNVERIFIED";
                }
            }

            String flagsJson;
            try {
                flagsJson = objectMapper.writeValueAsString(flags);
            } catch (JsonProcessingException e) {
                flagsJson = "[]";
            }

            // CRITICAL ACCEPTANCE CRITERIA:
            // "When complaint coordinates and OCR-extracted watermark coordinates are identical,
            // display approximately 0.0 m, but retain NEEDS_REVIEW because OCR watermark data is unverified."
            // "OCR confidence alone must never establish photo authenticity."
            String status = "NEEDS_REVIEW";
            double confidence = "OCR_WATERMARK_UNVERIFIED".equals(coordinateSource) ? Math.min(0.65, ocrResult.confidence()) : 0.50;

            // Update VerificationResult
            result.setCoordinateSource(coordinateSource);
            result.setDistanceMeters(distToReport);
            result.setOcrLatitude(ocrResult.ocrLatitude());
            result.setOcrLongitude(ocrResult.ocrLongitude());
            result.setOcrTimestamp(ocrResult.ocrTimestamp());
            result.setOcrWatermarkText(ocrResult.ocrRawText());
            result.setOcrWatermarkStatus(ocrResult.ocrStatus());
            result.setTimestampSource(timestampSource);
            result.setCheckReasons(flagsJson);
            result.setVerificationStatus(status);
            result.setConfidenceScore(BigDecimal.valueOf(confidence));
            result.setAnalysisNotes("Re-evaluated by admin " + adminEntity.getEmail() + " at " + OffsetDateTime.now() + ". Source: " + coordinateSource + ", distance: " + (distToReport != null ? String.format(Locale.ROOT, "%.1f", distToReport) + "m" : "N/A"));

            verificationResultRepository.save(result);

            // Update ComplaintImage
            image.setCoordinateSource(coordinateSource);
            image.setDistanceToReportMeters(distToReport);
            image.setOcrLatitude(ocrResult.ocrLatitude());
            image.setOcrLongitude(ocrResult.ocrLongitude());
            image.setOcrTimestamp(ocrResult.ocrTimestamp());
            image.setOcrWatermarkText(ocrResult.ocrRawText());
            image.setOcrWatermarkStatus(ocrResult.ocrStatus());
            image.setTimestampSource(timestampSource);
            image.setVerificationFlags(flagsJson);
            image.setVerificationStatus(status);
            complaintImageRepository.save(image);

            // Record progress update audit entry
            if (complaint != null) {
                String auditNote = "Admin re-evaluated verification evidence (ID: " + verificationId + "). Candidate coordinate source: " + coordinateSource +
                        ", distance: " + (distToReport != null ? String.format(Locale.ROOT, "%.1f", distToReport) + "m" : "N/A") +
                        ", OCR status: " + ocrResult.ocrStatus();
                ProgressUpdate auditLog = new ProgressUpdate(
                        complaint,
                        adminEntity,
                        complaint.getStatus(),
                        complaint.getStatus(),
                        "VERIFICATION_REEVALUATION",
                        auditNote
                );
                progressUpdateRepository.save(auditLog);
            }

            logger.info("[REEVALUATION_SUCCESS] Verification ID {} successfully re-evaluated. New distance: {}m, Source: {}",
                    verificationId, distToReport != null ? String.format(Locale.ROOT, "%.1f", distToReport) : "null", coordinateSource);

            return VerificationResultResponse.fromEntity(result);
        } finally {
            activeReevaluations.remove(verificationId);
        }
    }

    private byte[] downloadImageBytes(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("Image URL cannot be empty");
        }
        try {
            if (imageUrl.startsWith("file://") || imageUrl.matches("^[a-zA-Z]:\\\\.*")) {
                java.nio.file.Path p = java.nio.file.Paths.get(imageUrl.replace("file://", ""));
                return java.nio.file.Files.readAllBytes(p);
            }
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(10000);
            factory.setReadTimeout(15000);
            RestTemplate downloadTemplate = new RestTemplate(factory);
            byte[] bytes = downloadTemplate.getForObject(imageUrl, byte[].class);
            if (bytes == null || bytes.length == 0) {
                throw new IllegalStateException("Downloaded empty image payload from " + imageUrl);
            }
            return bytes;
        } catch (Exception e) {
            throw new RuntimeException("Failed to download image from URL: " + imageUrl + ": " + e.getMessage(), e);
        }
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
