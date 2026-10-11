package com.waste.management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "complaint_images")
public class ComplaintImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(name = "public_id", length = 255)
    private String publicId;

    @Column(name = "image_type", nullable = false, length = 40)
    private String imageType = "INITIAL_PROOF"; // INITIAL_PROOF, RESOLUTION_PROOF, INSPECTION_PROOF

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private OffsetDateTime uploadedAt = OffsetDateTime.now();

    @Column(name = "device_latitude", precision = 10, scale = 7)
    private BigDecimal deviceLatitude;

    @Column(name = "device_longitude", precision = 10, scale = 7)
    private BigDecimal deviceLongitude;

    @Column(name = "device_captured_at")
    private OffsetDateTime deviceCapturedAt;

    @Column(name = "exif_latitude", precision = 10, scale = 7)
    private BigDecimal exifLatitude;

    @Column(name = "exif_longitude", precision = 10, scale = 7)
    private BigDecimal exifLongitude;

    @Column(name = "exif_captured_at")
    private OffsetDateTime exifCapturedAt;

    @Column(name = "exif_camera_make", length = 100)
    private String exifCameraMake;

    @Column(name = "exif_camera_model", length = 100)
    private String exifCameraModel;

    @Column(name = "sha256_hash", length = 64)
    private String sha256Hash;

    @Column(name = "distance_to_report_meters")
    private Double distanceToReportMeters;

    @Column(name = "distance_device_to_exif_meters")
    private Double distanceDeviceToExifMeters;

    @Column(name = "verification_status", nullable = false, length = 40)
    private String verificationStatus = "VERIFIED"; // VERIFIED, NEEDS_REVIEW, REJECTED

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "verification_flags", columnDefinition = "jsonb")
    private String verificationFlags;

    @Column(name = "capture_source", nullable = false, length = 40)
    private String captureSource = "CAMERA"; // CAMERA, FILE_UPLOAD

    @Column(name = "file_last_modified_at")
    private OffsetDateTime fileLastModifiedAt;

    @Column(name = "device_gps_accuracy")
    private Double deviceGpsAccuracy;

    @Column(name = "device_gps_timestamp")
    private OffsetDateTime deviceGpsTimestamp;

    @Column(name = "coordinate_source", length = 50)
    private String coordinateSource;

    @Column(name = "timestamp_source", length = 50)
    private String timestampSource;

    @Column(name = "ocr_watermark_text", columnDefinition = "TEXT")
    private String ocrWatermarkText;

    @Column(name = "ocr_watermark_status", length = 50)
    private String ocrWatermarkStatus;

    public ComplaintImage() {}

    public ComplaintImage(Complaint complaint, String imageUrl, String publicId, String imageType, User uploadedBy) {
        this.complaint = complaint;
        this.imageUrl = imageUrl;
        this.publicId = publicId;
        this.imageType = imageType;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }

    public String getImageType() { return imageType; }
    public void setImageType(String imageType) { this.imageType = imageType; }

    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }

    public OffsetDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(OffsetDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public BigDecimal getDeviceLatitude() { return deviceLatitude; }
    public void setDeviceLatitude(BigDecimal deviceLatitude) { this.deviceLatitude = deviceLatitude; }

    public BigDecimal getDeviceLongitude() { return deviceLongitude; }
    public void setDeviceLongitude(BigDecimal deviceLongitude) { this.deviceLongitude = deviceLongitude; }

    public OffsetDateTime getDeviceCapturedAt() { return deviceCapturedAt; }
    public void setDeviceCapturedAt(OffsetDateTime deviceCapturedAt) { this.deviceCapturedAt = deviceCapturedAt; }

    public BigDecimal getExifLatitude() { return exifLatitude; }
    public void setExifLatitude(BigDecimal exifLatitude) { this.exifLatitude = exifLatitude; }

    public BigDecimal getExifLongitude() { return exifLongitude; }
    public void setExifLongitude(BigDecimal exifLongitude) { this.exifLongitude = exifLongitude; }

    public OffsetDateTime getExifCapturedAt() { return exifCapturedAt; }
    public void setExifCapturedAt(OffsetDateTime exifCapturedAt) { this.exifCapturedAt = exifCapturedAt; }

    public String getExifCameraMake() { return exifCameraMake; }
    public void setExifCameraMake(String exifCameraMake) { this.exifCameraMake = exifCameraMake; }

    public String getExifCameraModel() { return exifCameraModel; }
    public void setExifCameraModel(String exifCameraModel) { this.exifCameraModel = exifCameraModel; }

    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }

    public Double getDistanceToReportMeters() { return distanceToReportMeters; }
    public void setDistanceToReportMeters(Double distanceToReportMeters) { this.distanceToReportMeters = distanceToReportMeters; }

    public Double getDistanceDeviceToExifMeters() { return distanceDeviceToExifMeters; }
    public void setDistanceDeviceToExifMeters(Double distanceDeviceToExifMeters) { this.distanceDeviceToExifMeters = distanceDeviceToExifMeters; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getVerificationFlags() { return verificationFlags; }
    public void setVerificationFlags(String verificationFlags) { this.verificationFlags = verificationFlags; }

    public String getCaptureSource() { return captureSource; }
    public void setCaptureSource(String captureSource) { this.captureSource = captureSource; }

    public OffsetDateTime getFileLastModifiedAt() { return fileLastModifiedAt; }
    public void setFileLastModifiedAt(OffsetDateTime fileLastModifiedAt) { this.fileLastModifiedAt = fileLastModifiedAt; }

    public Double getDeviceGpsAccuracy() { return deviceGpsAccuracy; }
    public void setDeviceGpsAccuracy(Double deviceGpsAccuracy) { this.deviceGpsAccuracy = deviceGpsAccuracy; }

    public OffsetDateTime getDeviceGpsTimestamp() { return deviceGpsTimestamp; }
    public void setDeviceGpsTimestamp(OffsetDateTime deviceGpsTimestamp) { this.deviceGpsTimestamp = deviceGpsTimestamp; }

    public String getCoordinateSource() { return coordinateSource; }
    public void setCoordinateSource(String coordinateSource) { this.coordinateSource = coordinateSource; }

    public String getTimestampSource() { return timestampSource; }
    public void setTimestampSource(String timestampSource) { this.timestampSource = timestampSource; }

    public String getOcrWatermarkText() { return ocrWatermarkText; }
    public void setOcrWatermarkText(String ocrWatermarkText) { this.ocrWatermarkText = ocrWatermarkText; }

    public String getOcrWatermarkStatus() { return ocrWatermarkStatus; }
    public void setOcrWatermarkStatus(String ocrWatermarkStatus) { this.ocrWatermarkStatus = ocrWatermarkStatus; }
}
