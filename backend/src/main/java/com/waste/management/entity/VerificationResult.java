package com.waste.management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_results")
public class VerificationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private ComplaintImage image;

    @Column(name = "verifier_type", nullable = false, length = 30)
    private String verifierType = "GPS_VERIFIER"; // GPS_VERIFIER, AI_SERVICE, ADMIN_USER, ADMIN_REVIEW

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verifier_user_id")
    private User verifierUser;

    @Column(name = "verification_status", nullable = false, length = 40)
    private String verificationStatus = "VERIFIED"; // VERIFIED, NEEDS_REVIEW, REJECTED

    @Column(name = "check_type", nullable = false, length = 40)
    private String checkType = "INITIAL_REPORT"; // INITIAL_REPORT, CLEANUP_RESOLUTION

    @Column(name = "is_valid_plastic", nullable = false)
    private Boolean isValidPlastic = true;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore = BigDecimal.valueOf(1.0);

    @Column(name = "device_latitude", precision = 10, scale = 7)
    private BigDecimal deviceLatitude;

    @Column(name = "device_longitude", precision = 10, scale = 7)
    private BigDecimal deviceLongitude;

    @Column(name = "exif_latitude", precision = 10, scale = 7)
    private BigDecimal exifLatitude;

    @Column(name = "exif_longitude", precision = 10, scale = 7)
    private BigDecimal exifLongitude;

    @Column(name = "target_latitude", precision = 10, scale = 7)
    private BigDecimal targetLatitude;

    @Column(name = "target_longitude", precision = 10, scale = 7)
    private BigDecimal targetLongitude;

    @Column(name = "distance_meters")
    private Double distanceMeters;

    @Column(name = "distance_tolerance_meters")
    private Double distanceToleranceMeters;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "check_reasons", columnDefinition = "jsonb")
    private String checkReasons;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "detected_classes", columnDefinition = "jsonb")
    private String detectedClasses;

    @Column(name = "analysis_notes", columnDefinition = "TEXT")
    private String analysisNotes;

    @Column(name = "review_decision", length = 30)
    private String reviewDecision = "PENDING"; // PENDING, APPROVED, REJECTED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "verified_at", nullable = false, updatable = false)
    private OffsetDateTime verifiedAt = OffsetDateTime.now();

    public VerificationResult() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }

    public Assignment getAssignment() { return assignment; }
    public void setAssignment(Assignment assignment) { this.assignment = assignment; }

    public ComplaintImage getImage() { return image; }
    public void setImage(ComplaintImage image) { this.image = image; }

    public String getVerifierType() { return verifierType; }
    public void setVerifierType(String verifierType) { this.verifierType = verifierType; }

    public User getVerifierUser() { return verifierUser; }
    public void setVerifierUser(User verifierUser) { this.verifierUser = verifierUser; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getCheckType() { return checkType; }
    public void setCheckType(String checkType) { this.checkType = checkType; }

    public Boolean getIsValidPlastic() { return isValidPlastic; }
    public void setIsValidPlastic(Boolean isValidPlastic) { this.isValidPlastic = isValidPlastic; }

    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }

    public BigDecimal getDeviceLatitude() { return deviceLatitude; }
    public void setDeviceLatitude(BigDecimal deviceLatitude) { this.deviceLatitude = deviceLatitude; }

    public BigDecimal getDeviceLongitude() { return deviceLongitude; }
    public void setDeviceLongitude(BigDecimal deviceLongitude) { this.deviceLongitude = deviceLongitude; }

    public BigDecimal getExifLatitude() { return exifLatitude; }
    public void setExifLatitude(BigDecimal exifLatitude) { this.exifLatitude = exifLatitude; }

    public BigDecimal getExifLongitude() { return exifLongitude; }
    public void setExifLongitude(BigDecimal exifLongitude) { this.exifLongitude = exifLongitude; }

    public BigDecimal getTargetLatitude() { return targetLatitude; }
    public void setTargetLatitude(BigDecimal targetLatitude) { this.targetLatitude = targetLatitude; }

    public BigDecimal getTargetLongitude() { return targetLongitude; }
    public void setTargetLongitude(BigDecimal targetLongitude) { this.targetLongitude = targetLongitude; }

    public Double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Double distanceMeters) { this.distanceMeters = distanceMeters; }

    public Double getDistanceToleranceMeters() { return distanceToleranceMeters; }
    public void setDistanceToleranceMeters(Double distanceToleranceMeters) { this.distanceToleranceMeters = distanceToleranceMeters; }

    public String getCheckReasons() { return checkReasons; }
    public void setCheckReasons(String checkReasons) { this.checkReasons = checkReasons; }

    public String getDetectedClasses() { return detectedClasses; }
    public void setDetectedClasses(String detectedClasses) { this.detectedClasses = detectedClasses; }

    public String getAnalysisNotes() { return analysisNotes; }
    public void setAnalysisNotes(String analysisNotes) { this.analysisNotes = analysisNotes; }

    public String getReviewDecision() { return reviewDecision; }
    public void setReviewDecision(String reviewDecision) { this.reviewDecision = reviewDecision; }

    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }

    public OffsetDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(OffsetDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public OffsetDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(OffsetDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
}
