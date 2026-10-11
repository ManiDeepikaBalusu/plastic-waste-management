package com.waste.management.service;

import com.waste.management.dto.*;
import com.waste.management.entity.Complaint;
import com.waste.management.entity.ComplaintImage;
import com.waste.management.entity.ProgressUpdate;
import com.waste.management.entity.User;
import com.waste.management.exception.ResourceNotFoundException;
import com.waste.management.exception.UnauthorizedActionException;
import com.waste.management.repository.ComplaintImageRepository;
import com.waste.management.repository.ComplaintRepository;
import com.waste.management.repository.ProgressUpdateRepository;
import com.waste.management.repository.UserRepository;
import com.waste.management.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintImageRepository complaintImageRepository;
    private final ProgressUpdateRepository progressUpdateRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final MediaService mediaService;
    private final ImageValidator imageValidator;
    private final PhotoVerificationService photoVerificationService;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            ComplaintImageRepository complaintImageRepository,
            ProgressUpdateRepository progressUpdateRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            MediaService mediaService,
            ImageValidator imageValidator,
            PhotoVerificationService photoVerificationService) {
        this.complaintRepository = complaintRepository;
        this.complaintImageRepository = complaintImageRepository;
        this.progressUpdateRepository = progressUpdateRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.mediaService = mediaService;
        this.imageValidator = imageValidator;
        this.photoVerificationService = photoVerificationService;
    }

    @Transactional(rollbackFor = Exception.class)
    public ComplaintResponse createComplaintWithPhotos(
            CreateComplaintRequest request,
            List<MultipartFile> files,
            UserPrincipal currentUser) {

        // Validate Coordinates Boundary
        validateCoordinates(request.latitude(), request.longitude());

        // Validate Image Files & Limits
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one initial proof photo must be uploaded with the complaint.");
        }

        if (files.size() > ImageValidator.MAX_IMAGES_PER_COMPLAINT) {
            throw new IllegalArgumentException("Cannot upload more than " + ImageValidator.MAX_IMAGES_PER_COMPLAINT + " images per complaint.");
        }

        // Security Check: Magic bytes, file size, and MIME validation for every file
        for (MultipartFile file : files) {
            imageValidator.validateImageFile(file);
        }

        User citizen = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUser.getId()));

        Complaint complaint = new Complaint();
        complaint.setCitizen(citizen);
        complaint.setTitle(request.title().trim());
        complaint.setDescription(request.description().trim());
        complaint.setWasteType(request.wasteType() != null ? request.wasteType().trim() : "PLASTIC_MIXED");
        complaint.setEstimatedSize(request.estimatedSize() != null ? request.estimatedSize().trim() : "MEDIUM_PILE");
        complaint.setLatitude(request.latitude());
        complaint.setLongitude(request.longitude());
        complaint.setAddressText(request.addressText() != null ? request.addressText().trim() : null);
        complaint.setStatus("SUBMITTED");

        Complaint savedComplaint = complaintRepository.save(complaint);

        // Upload images to Cloudinary and attach records
        boolean anyFlagged = false;
        StringBuilder flagSummary = new StringBuilder();

        for (MultipartFile file : files) {
            MediaUploadResponse uploadRes = mediaService.uploadImage(file);
            ComplaintImage img = new ComplaintImage(
                    savedComplaint,
                    uploadRes.imageUrl(),
                    uploadRes.publicId(),
                    "INITIAL_PROOF",
                    citizen
            );
            img.setBrowserUploadLatitude(request.browserUploadLatitude());
            img.setBrowserUploadLongitude(request.browserUploadLongitude());
            ComplaintImage savedImg = complaintImageRepository.save(img);

            // Execute automated GPS and EXIF verification
            PhotoVerificationResultDto verifResult = photoVerificationService.verifyInitialPhoto(
                    file,
                    savedComplaint,
                    savedImg,
                    request.deviceLatitude(),
                    request.deviceLongitude(),
                    request.deviceCapturedAt(),
                    request.captureSource(),
                    request.deviceAccuracy(),
                    request.deviceGpsTimestamp(),
                    request.fileLastModifiedAt(),
                    request.browserUploadLatitude(),
                    request.browserUploadLongitude(),
                    citizen
            );

            if ("NEEDS_REVIEW".equalsIgnoreCase(verifResult.verificationStatus()) || "REJECTED".equalsIgnoreCase(verifResult.verificationStatus())) {
                anyFlagged = true;
                flagSummary.append(String.join(", ", verifResult.flags())).append("; ");
            }

            savedComplaint.getImages().add(savedImg);
        }

        savedComplaint.setStatus("SUBMITTED");
        savedComplaint = complaintRepository.save(savedComplaint);

        // Record progress update audit trail
        String auditMessage = anyFlagged
                ? "Plastic waste report submitted with " + files.size() + " photo(s). Automated verification flagged for review: " + flagSummary
                : "Plastic waste report submitted with " + files.size() + " photo(s). Auto-verified coordinates.";
        ProgressUpdate log = new ProgressUpdate(
                savedComplaint,
                citizen,
                null,
                "SUBMITTED",
                anyFlagged ? "FLAGGED_FOR_REVIEW" : "SUBMISSION",
                auditMessage
        );
        progressUpdateRepository.save(log);

        // Dispatch in-app notification
        notificationService.createNotification(
                citizen,
                "Complaint Filed Successfully",
                anyFlagged
                        ? "Your report '" + savedComplaint.getTitle() + "' was submitted and flagged for administrative verification."
                        : "Your plastic waste report '" + savedComplaint.getTitle() + "' has been submitted for review.",
                "COMPLAINT_SUBMITTED",
                savedComplaint.getId()
        );

        return ComplaintResponse.fromEntity(savedComplaint);
    }

    @Transactional(rollbackFor = Exception.class)
    public ComplaintResponse createComplaint(CreateComplaintRequest request, UserPrincipal currentUser) {
        validateCoordinates(request.latitude(), request.longitude());

        User citizen = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUser.getId()));

        Complaint complaint = new Complaint();
        complaint.setCitizen(citizen);
        complaint.setTitle(request.title().trim());
        complaint.setDescription(request.description().trim());
        complaint.setWasteType(request.wasteType() != null ? request.wasteType().trim() : "PLASTIC_MIXED");
        complaint.setEstimatedSize(request.estimatedSize() != null ? request.estimatedSize().trim() : "MEDIUM_PILE");
        complaint.setLatitude(request.latitude());
        complaint.setLongitude(request.longitude());
        complaint.setAddressText(request.addressText() != null ? request.addressText().trim() : null);
        complaint.setStatus("SUBMITTED");

        Complaint savedComplaint = complaintRepository.save(complaint);

        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            for (String url : request.imageUrls()) {
                if (url != null && !url.isBlank()) {
                    ComplaintImage img = new ComplaintImage(savedComplaint, url.trim(), null, "INITIAL_PROOF", citizen);
                    complaintImageRepository.save(img);
                    savedComplaint.getImages().add(img);
                }
            }
        }

        ProgressUpdate log = new ProgressUpdate(
                savedComplaint,
                citizen,
                null,
                "SUBMITTED",
                "SUBMISSION",
                "Plastic waste report submitted by citizen."
        );
        progressUpdateRepository.save(log);

        notificationService.createNotification(
                citizen,
                "Complaint Filed Successfully",
                "Your plastic waste report '" + savedComplaint.getTitle() + "' has been submitted for review.",
                "COMPLAINT_SUBMITTED",
                savedComplaint.getId()
        );

        return ComplaintResponse.fromEntity(savedComplaint);
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getCitizenComplaints(UserPrincipal currentUser) {
        return complaintRepository.findByCitizenId(currentUser.getId()).stream()
                .map(ComplaintResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(UUID complaintId, UserPrincipal currentUser) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        boolean isOwner = complaint.getCitizen().getId().equals(currentUser.getId());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
        boolean isVolunteer = "VOLUNTEER".equalsIgnoreCase(currentUser.getRole());

        if (!isOwner && !isAdmin && !isVolunteer) {
            throw new UnauthorizedActionException("Access Denied: You are not authorized to view this complaint.");
        }

        return ComplaintResponse.fromEntity(complaint);
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getAllComplaints(String status) {
        List<Complaint> list;
        if (status != null && !status.isBlank()) {
            list = complaintRepository.findByStatus(status.trim().toUpperCase());
        } else {
            list = complaintRepository.findAll();
        }
        return list.stream().map(ComplaintResponse::fromEntity).toList();
    }

    @Transactional
    public ComplaintResponse markUnderReview(UUID complaintId, String remarks, UserPrincipal adminUser) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        if (!"SUBMITTED".equalsIgnoreCase(complaint.getStatus())) {
            throw new IllegalStateException("Cannot move to UNDER_REVIEW: Current status is '" + complaint.getStatus() + "'. Expected 'SUBMITTED'.");
        }

        User actor = userRepository.findById(adminUser.getId()).orElseThrow();
        String previousStatus = complaint.getStatus();
        complaint.setStatus("UNDER_REVIEW");
        Complaint updated = complaintRepository.save(complaint);

        String note = (remarks != null && !remarks.isBlank()) ? remarks.trim() : "Admin initiated triage evaluation.";
        progressUpdateRepository.save(new ProgressUpdate(updated, actor, previousStatus, "UNDER_REVIEW", "TRIAGE", note));

        notificationService.createNotification(
                updated.getCitizen(),
                "Complaint Under Review",
                "Your report '" + updated.getTitle() + "' is currently under administrative evaluation.",
                "COMPLAINT_UNDER_REVIEW",
                updated.getId()
        );

        return ComplaintResponse.fromEntity(updated);
    }

    @Transactional
    public ComplaintResponse verifyComplaint(UUID complaintId, String remarks, UserPrincipal adminUser) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        if (!"UNDER_REVIEW".equalsIgnoreCase(complaint.getStatus()) && !"SUBMITTED".equalsIgnoreCase(complaint.getStatus())) {
            throw new IllegalStateException("Cannot verify complaint: Current status is '" + complaint.getStatus() + "'. Expected 'UNDER_REVIEW' or 'SUBMITTED'.");
        }

        User actor = userRepository.findById(adminUser.getId()).orElseThrow();
        String previousStatus = complaint.getStatus();
        complaint.setStatus("VERIFIED");
        Complaint updated = complaintRepository.save(complaint);

        String note = (remarks != null && !remarks.isBlank()) ? remarks.trim() : "Admin verified valid plastic waste site.";
        progressUpdateRepository.save(new ProgressUpdate(updated, actor, previousStatus, "VERIFIED", "VERIFICATION", note));

        notificationService.createNotification(
                updated.getCitizen(),
                "Complaint Verified",
                "Your report '" + updated.getTitle() + "' has been verified and queued for volunteer dispatch.",
                "COMPLAINT_VERIFIED",
                updated.getId()
        );

        return ComplaintResponse.fromEntity(updated);
    }

    @Transactional
    public ComplaintResponse rejectComplaint(UUID complaintId, ComplaintRejectRequest request, UserPrincipal adminUser) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        if (!"SUBMITTED".equalsIgnoreCase(complaint.getStatus()) && !"UNDER_REVIEW".equalsIgnoreCase(complaint.getStatus())) {
            throw new IllegalStateException("Cannot reject complaint: Current status is '" + complaint.getStatus() + "'. Expected 'SUBMITTED' or 'UNDER_REVIEW'.");
        }

        User actor = userRepository.findById(adminUser.getId()).orElseThrow();
        String previousStatus = complaint.getStatus();
        complaint.setStatus("REJECTED");
        complaint.setRejectionReason(request.rejectionReason().trim());
        Complaint updated = complaintRepository.save(complaint);

        progressUpdateRepository.save(new ProgressUpdate(updated, actor, previousStatus, "REJECTED", "REJECTION", request.rejectionReason().trim()));

        notificationService.createNotification(
                updated.getCitizen(),
                "Complaint Rejected",
                "Your report '" + updated.getTitle() + "' was rejected. Reason: " + request.rejectionReason().trim(),
                "COMPLAINT_REJECTED",
                updated.getId()
        );

        return ComplaintResponse.fromEntity(updated);
    }

    @Transactional
    public ComplaintResponse cancelComplaint(UUID complaintId, ComplaintCancelRequest request, UserPrincipal currentUser) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        boolean isOwner = complaint.getCitizen().getId().equals(currentUser.getId());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedActionException("Access Denied: You are not authorized to cancel this complaint.");
        }

        if (!"SUBMITTED".equalsIgnoreCase(complaint.getStatus()) && !"UNDER_REVIEW".equalsIgnoreCase(complaint.getStatus())) {
            throw new IllegalStateException("Cannot cancel complaint: Report is already in state '" + complaint.getStatus() + "'. Cancellation is only permitted prior to verification/dispatch.");
        }

        User actor = userRepository.findById(currentUser.getId()).orElseThrow();
        String previousStatus = complaint.getStatus();
        complaint.setStatus("CANCELLED");
        complaint.setCancellationReason(request.cancellationReason().trim());
        Complaint updated = complaintRepository.save(complaint);

        progressUpdateRepository.save(new ProgressUpdate(updated, actor, previousStatus, "CANCELLED", "CANCELLATION", request.cancellationReason().trim()));

        notificationService.createNotification(
                updated.getCitizen(),
                "Complaint Cancelled",
                "Report '" + updated.getTitle() + "' has been cancelled.",
                "COMPLAINT_CANCELLED",
                updated.getId()
        );

        return ComplaintResponse.fromEntity(updated);
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || latitude.compareTo(new BigDecimal("-90.0")) < 0 || latitude.compareTo(new BigDecimal("90.0")) > 0) {
            throw new IllegalArgumentException("Latitude value '" + latitude + "' is out of valid range [-90.0, 90.0].");
        }
        if (longitude == null || longitude.compareTo(new BigDecimal("-180.0")) < 0 || longitude.compareTo(new BigDecimal("180.0")) > 0) {
            throw new IllegalArgumentException("Longitude value '" + longitude + "' is out of valid range [-180.0, 180.0].");
        }
    }
}
