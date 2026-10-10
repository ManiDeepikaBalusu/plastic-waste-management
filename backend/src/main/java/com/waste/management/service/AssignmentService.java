package com.waste.management.service;

import com.waste.management.dto.AssignmentResponse;
import com.waste.management.dto.CreateAssignmentRequest;
import com.waste.management.dto.MediaUploadResponse;
import com.waste.management.entity.*;
import com.waste.management.exception.ResourceNotFoundException;
import com.waste.management.exception.UnauthorizedActionException;
import com.waste.management.repository.*;
import com.waste.management.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final ComplaintImageRepository complaintImageRepository;
    private final ProgressUpdateRepository progressUpdateRepository;
    private final NotificationService notificationService;
    private final MediaService mediaService;
    private final ImageValidator imageValidator;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            ComplaintRepository complaintRepository,
            UserRepository userRepository,
            VolunteerProfileRepository volunteerProfileRepository,
            ComplaintImageRepository complaintImageRepository,
            ProgressUpdateRepository progressUpdateRepository,
            NotificationService notificationService,
            MediaService mediaService,
            ImageValidator imageValidator) {
        this.assignmentRepository = assignmentRepository;
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.volunteerProfileRepository = volunteerProfileRepository;
        this.complaintImageRepository = complaintImageRepository;
        this.progressUpdateRepository = progressUpdateRepository;
        this.notificationService = notificationService;
        this.mediaService = mediaService;
        this.imageValidator = imageValidator;
    }

    @Transactional(rollbackFor = Exception.class)
    public AssignmentResponse assignComplaint(CreateAssignmentRequest request, UserPrincipal adminUser) {
        Complaint complaint = complaintRepository.findById(request.complaintId())
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + request.complaintId()));

        if ("RESOLVED".equalsIgnoreCase(complaint.getStatus()) || "REJECTED".equalsIgnoreCase(complaint.getStatus())) {
            throw new IllegalStateException("Cannot assign complaint: Status is '" + complaint.getStatus() + "'. Closed or rejected complaints cannot be assigned.");
        }

        User volunteer = userRepository.findById(request.volunteerId())
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer user not found with id: " + request.volunteerId()));

        if (!"VOLUNTEER".equalsIgnoreCase(volunteer.getRole())) {
            throw new IllegalArgumentException("User " + volunteer.getEmail() + " does not have role 'VOLUNTEER'.");
        }

        User admin = userRepository.findById(adminUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        // Cancel any existing active assignments for this complaint if reassigning
        List<Assignment> existing = assignmentRepository.findByComplaintId(complaint.getId());
        for (Assignment prev : existing) {
            if ("PENDING".equalsIgnoreCase(prev.getStatus()) || "ACCEPTED".equalsIgnoreCase(prev.getStatus())) {
                prev.setStatus("REASSIGNED");
                prev.setCancelledAt(OffsetDateTime.now());
                assignmentRepository.save(prev);
            }
        }

        String previousStatus = complaint.getStatus();
        complaint.setStatus("ASSIGNED");
        complaintRepository.save(complaint);

        Assignment assignment = new Assignment(complaint, volunteer, admin);
        assignment.setNotes(request.notes());
        Assignment saved = assignmentRepository.save(assignment);

        // Update volunteer profile assignment counter
        volunteerProfileRepository.findByUserId(volunteer.getId()).ifPresent(p -> {
            p.setTotalAssigned(p.getTotalAssigned() + 1);
            volunteerProfileRepository.save(p);
        });

        // Audit Trail
        progressUpdateRepository.save(new ProgressUpdate(
                complaint,
                admin,
                previousStatus,
                "ASSIGNED",
                "ASSIGNMENT",
                "Assigned to volunteer " + volunteer.getFullName() + ". Notes: " + (request.notes() != null ? request.notes() : "None")
        ));

        // Notifications
        notificationService.createNotification(
                volunteer,
                "New Cleanup Task Assigned",
                "You have been assigned to clean up: " + complaint.getTitle(),
                "TASK_ASSIGNED",
                saved.getId()
        );

        if (complaint.getCitizen() != null) {
            notificationService.createNotification(
                    complaint.getCitizen(),
                    "Task Dispatched to Volunteer",
                    "A community volunteer has been dispatched to address your report: " + complaint.getTitle(),
                    "TASK_DISPATCHED",
                    complaint.getId()
            );
        }

        return AssignmentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getMyAssignments(UserPrincipal volunteerUser) {
        return assignmentRepository.findByVolunteerId(volunteerUser.getId()).stream()
                .map(AssignmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentById(UUID assignmentId, UserPrincipal currentUser) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

        boolean isVolunteer = assignment.getVolunteer().getId().equals(currentUser.getId());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());
        boolean isCitizen = assignment.getComplaint().getCitizen() != null &&
                assignment.getComplaint().getCitizen().getId().equals(currentUser.getId());

        if (!isVolunteer && !isAdmin && !isCitizen) {
            throw new UnauthorizedActionException("Access Denied: You are not authorized to view this task.");
        }

        return AssignmentResponse.fromEntity(assignment);
    }

    @Transactional
    public AssignmentResponse acceptAssignment(UUID assignmentId, UserPrincipal volunteerUser) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

        if (!assignment.getVolunteer().getId().equals(volunteerUser.getId())) {
            throw new UnauthorizedActionException("Access Denied: You are not the assigned volunteer for this task.");
        }

        if (!"PENDING".equalsIgnoreCase(assignment.getStatus())) {
            throw new IllegalStateException("Cannot accept assignment: Status is '" + assignment.getStatus() + "'. Expected 'PENDING'.");
        }

        assignment.setStatus("ACCEPTED");
        assignment.setAcceptedAt(OffsetDateTime.now());
        Assignment updated = assignmentRepository.save(assignment);

        Complaint complaint = assignment.getComplaint();
        complaint.setStatus("ACCEPTED");
        complaintRepository.save(complaint);

        User vol = userRepository.findById(volunteerUser.getId()).orElseThrow();
        progressUpdateRepository.save(new ProgressUpdate(
                complaint,
                vol,
                "ASSIGNED",
                "ACCEPTED",
                "STATUS_CHANGE",
                "Volunteer accepted cleanup assignment."
        ));

        return AssignmentResponse.fromEntity(updated);
    }

    @Transactional
    public AssignmentResponse declineAssignment(UUID assignmentId, String reason, UserPrincipal volunteerUser) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

        if (!assignment.getVolunteer().getId().equals(volunteerUser.getId())) {
            throw new UnauthorizedActionException("Access Denied: You are not the assigned volunteer for this task.");
        }

        assignment.setStatus("DECLINED");
        assignment.setCancelledAt(OffsetDateTime.now());
        assignment.setNotes(reason != null ? "Declined: " + reason : "Declined by volunteer");
        Assignment updated = assignmentRepository.save(assignment);

        Complaint complaint = assignment.getComplaint();
        complaint.setStatus("VERIFIED"); // Re-queue for next volunteer
        complaintRepository.save(complaint);

        User vol = userRepository.findById(volunteerUser.getId()).orElseThrow();
        progressUpdateRepository.save(new ProgressUpdate(
                complaint,
                vol,
                "ASSIGNED",
                "VERIFIED",
                "STATUS_CHANGE",
                "Volunteer declined task. Reset to VERIFIED for re-dispatch. Reason: " + (reason != null ? reason : "None")
        ));

        return AssignmentResponse.fromEntity(updated);
    }

    @Transactional
    public AssignmentResponse startAssignment(UUID assignmentId, UserPrincipal volunteerUser) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

        if (!assignment.getVolunteer().getId().equals(volunteerUser.getId())) {
            throw new UnauthorizedActionException("Access Denied: You are not the assigned volunteer for this task.");
        }

        assignment.setStatus("IN_PROGRESS");
        Assignment updated = assignmentRepository.save(assignment);

        Complaint complaint = assignment.getComplaint();
        complaint.setStatus("IN_PROGRESS");
        complaintRepository.save(complaint);

        User vol = userRepository.findById(volunteerUser.getId()).orElseThrow();
        progressUpdateRepository.save(new ProgressUpdate(
                complaint,
                vol,
                "ACCEPTED",
                "IN_PROGRESS",
                "STATUS_CHANGE",
                "Volunteer began active on-site cleanup work."
        ));

        return AssignmentResponse.fromEntity(updated);
    }

    @Transactional(rollbackFor = Exception.class)
    public AssignmentResponse resolveAssignmentWithProof(
            UUID assignmentId,
            String notes,
            List<MultipartFile> files,
            UserPrincipal volunteerUser) {

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));

        if (!assignment.getVolunteer().getId().equals(volunteerUser.getId())) {
            throw new UnauthorizedActionException("Access Denied: You are not the assigned volunteer for this task.");
        }

        User vol = userRepository.findById(volunteerUser.getId()).orElseThrow();
        Complaint complaint = assignment.getComplaint();

        // Process proof photos if provided
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                imageValidator.validateImageFile(file);
                MediaUploadResponse res = mediaService.uploadImage(file);
                ComplaintImage proofImg = new ComplaintImage(complaint, res.imageUrl(), res.publicId(), "RESOLUTION_PROOF", vol);
                complaintImageRepository.save(proofImg);
            }
        }

        assignment.setStatus("COMPLETED");
        assignment.setCompletedAt(OffsetDateTime.now());
        if (notes != null && !notes.isBlank()) {
            assignment.setNotes(notes.trim());
        }
        Assignment updated = assignmentRepository.save(assignment);

        complaint.setStatus("RESOLVED");
        complaintRepository.save(complaint);

        // Update Volunteer Profile resolved counter
        volunteerProfileRepository.findByUserId(vol.getId()).ifPresent(p -> {
            p.setTotalResolved(p.getTotalResolved() + 1);
            volunteerProfileRepository.save(p);
        });

        // Audit Trail
        progressUpdateRepository.save(new ProgressUpdate(
                complaint,
                vol,
                "IN_PROGRESS",
                "RESOLVED",
                "RESOLUTION",
                "Plastic waste cleanup successfully resolved by volunteer. Proof photos attached."
        ));

        // Notify Citizen
        if (complaint.getCitizen() != null) {
            notificationService.createNotification(
                    complaint.getCitizen(),
                    "Waste Report Resolved!",
                    "Great news! Your plastic waste report '" + complaint.getTitle() + "' has been cleared and verified.",
                    "COMPLAINT_RESOLVED",
                    complaint.getId()
            );
        }

        return AssignmentResponse.fromEntity(updated);
    }
}
