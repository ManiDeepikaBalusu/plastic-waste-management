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

    @Column(name = "verifier_type", nullable = false, length = 30)
    private String verifierType; // AI_SERVICE, ADMIN_USER

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verifier_user_id")
    private User verifierUser;

    @Column(name = "is_valid_plastic", nullable = false)
    private Boolean isValidPlastic;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "detected_classes", columnDefinition = "jsonb")
    private String detectedClasses; // JSON representation of detected labels & scores

    @Column(name = "analysis_notes", columnDefinition = "TEXT")
    private String analysisNotes;

    @Column(name = "verified_at", nullable = false, updatable = false)
    private OffsetDateTime verifiedAt = OffsetDateTime.now();

    public VerificationResult() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }

    public String getVerifierType() { return verifierType; }
    public void setVerifierType(String verifierType) { this.verifierType = verifierType; }

    public User getVerifierUser() { return verifierUser; }
    public void setVerifierUser(User verifierUser) { this.verifierUser = verifierUser; }

    public Boolean getIsValidPlastic() { return isValidPlastic; }
    public void setIsValidPlastic(Boolean isValidPlastic) { this.isValidPlastic = isValidPlastic; }

    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getDetectedClasses() { return detectedClasses; }
    public void setDetectedClasses(String detectedClasses) { this.detectedClasses = detectedClasses; }

    public String getAnalysisNotes() { return analysisNotes; }
    public void setAnalysisNotes(String analysisNotes) { this.analysisNotes = analysisNotes; }

    public OffsetDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(OffsetDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
}
