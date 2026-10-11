package com.waste.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waste.management.dto.VerificationDecisionRequest;
import com.waste.management.entity.*;
import com.waste.management.repository.*;
import com.waste.management.security.JwtTokenProvider;
import com.waste.management.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PhotoVerificationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private VerificationResultRepository verificationResultRepository;

    @Autowired
    private ComplaintImageRepository complaintImageRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProgressUpdateRepository progressUpdateRepository;

    @Autowired
    private VolunteerProfileRepository volunteerProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User citizen;
    private User otherCitizen;
    private User volunteer;
    private User admin;

    private String tokenCitizen;
    private String tokenOtherCitizen;
    private String tokenVolunteer;
    private String tokenAdmin;

    private Complaint testComplaint;
    private Assignment testAssignment;
    private VerificationResult testVerifResult;

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

        citizen = userRepository.save(new User("citizen_api@test.org", passwordEncoder.encode("Pass123!"), "Api Citizen", "+11111", "CITIZEN"));
        otherCitizen = userRepository.save(new User("other_api@test.org", passwordEncoder.encode("Pass123!"), "Other Citizen", "+44444", "CITIZEN"));
        volunteer = userRepository.save(new User("vol_api@test.org", passwordEncoder.encode("Pass123!"), "Api Volunteer", "+22222", "VOLUNTEER"));
        admin = userRepository.save(new User("admin_api@test.org", passwordEncoder.encode("Pass123!"), "Api Admin", "+33333", "ADMIN"));

        tokenCitizen = tokenProvider.generateToken(UserPrincipal.create(citizen));
        tokenOtherCitizen = tokenProvider.generateToken(UserPrincipal.create(otherCitizen));
        tokenVolunteer = tokenProvider.generateToken(UserPrincipal.create(volunteer));
        tokenAdmin = tokenProvider.generateToken(UserPrincipal.create(admin));

        testComplaint = new Complaint();
        testComplaint.setCitizen(citizen);
        testComplaint.setTitle("Api Test Waste");
        testComplaint.setDescription("Waste on sidewalk");
        testComplaint.setLatitude(new BigDecimal("12.9716"));
        testComplaint.setLongitude(new BigDecimal("77.5946"));
        testComplaint.setStatus("UNDER_REVIEW");
        testComplaint = complaintRepository.save(testComplaint);

        testAssignment = new Assignment(testComplaint, volunteer, admin);
        testAssignment.setStatus("IN_PROGRESS");
        testAssignment = assignmentRepository.save(testAssignment);

        testVerifResult = new VerificationResult();
        testVerifResult.setComplaint(testComplaint);
        testVerifResult.setAssignment(testAssignment);
        testVerifResult.setVerifierType("GPS_VERIFIER");
        testVerifResult.setVerifierUser(citizen);
        testVerifResult.setVerificationStatus("NEEDS_REVIEW");
        testVerifResult.setCheckType("INITIAL_REPORT");
        testVerifResult.setIsValidPlastic(true);
        testVerifResult.setConfidenceScore(new BigDecimal("0.50"));
        testVerifResult.setReviewDecision("PENDING");
        testVerifResult = verificationResultRepository.save(testVerifResult);
    }

    @Test
    @DisplayName("Security: Citizens cannot access Admin verification decision endpoint (403 Forbidden)")
    void shouldDenyCitizenAccessToAdminDecision() throws Exception {
        VerificationDecisionRequest request = new VerificationDecisionRequest("APPROVE", "Citizen attempting override");

        mockMvc.perform(post("/api/v1/verifications/" + testVerifResult.getId() + "/decision")
                        .header("Authorization", "Bearer " + tokenCitizen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Volunteers cannot access Admin verification decision endpoint (403 Forbidden)")
    void shouldDenyVolunteerAccessToAdminDecision() throws Exception {
        VerificationDecisionRequest request = new VerificationDecisionRequest("APPROVE", "Volunteer attempting override");

        mockMvc.perform(post("/api/v1/verifications/" + testVerifResult.getId() + "/decision")
                        .header("Authorization", "Bearer " + tokenVolunteer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Unrelated citizens cannot view other users' verification records (403 Forbidden)")
    void shouldDenyUnauthorizedCitizenFromViewingOtherComplaintVerifications() throws Exception {
        mockMvc.perform(get("/api/v1/verifications/complaint/" + testComplaint.getId())
                        .header("Authorization", "Bearer " + tokenOtherCitizen))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authorized Access: Owner citizen and Admin can view verification records")
    void shouldAllowOwnerAndAdminToViewVerifications() throws Exception {
        // Owner Citizen
        mockMvc.perform(get("/api/v1/verifications/complaint/" + testComplaint.getId())
                        .header("Authorization", "Bearer " + tokenCitizen))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Admin
        mockMvc.perform(get("/api/v1/verifications/complaint/" + testComplaint.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Admin Decision: Admin successfully records decision with mandatory reason")
    void shouldAllowAdminToRecordDecision() throws Exception {
        VerificationDecisionRequest request = new VerificationDecisionRequest(
                "APPROVE", "Verified by site manager through secondary inspection."
        );

        mockMvc.perform(post("/api/v1/verifications/" + testVerifResult.getId() + "/decision")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewDecision").value("APPROVED"))
                .andExpect(jsonPath("$.adminNotes").value("Verified by site manager through secondary inspection."));
    }

    @Test
    @DisplayName("Validation: Reject Admin decision when mandatory reason is missing (400 Bad Request)")
    void shouldRejectDecisionWhenReasonIsMissing() throws Exception {
        VerificationDecisionRequest request = new VerificationDecisionRequest("APPROVE", "   ");

        mockMvc.perform(post("/api/v1/verifications/" + testVerifResult.getId() + "/decision")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
