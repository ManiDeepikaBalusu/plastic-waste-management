package com.waste.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waste.management.dto.ComplaintCancelRequest;
import com.waste.management.dto.ComplaintRejectRequest;
import com.waste.management.dto.CreateComplaintRequest;
import com.waste.management.entity.Complaint;
import com.waste.management.entity.ProgressUpdate;
import com.waste.management.entity.User;
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

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComplaintLifecycleSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VolunteerProfileRepository volunteerProfileRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintImageRepository complaintImageRepository;

    @Autowired
    private ProgressUpdateRepository progressUpdateRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User citizenA;
    private User citizenB;
    private User admin;
    private String tokenCitizenA;
    private String tokenCitizenB;
    private String tokenAdmin;

    @BeforeEach
    void setUp() {
        // Delete in reverse foreign key order
        notificationRepository.deleteAll();
        progressUpdateRepository.deleteAll();
        complaintImageRepository.deleteAll();
        complaintRepository.deleteAll();
        volunteerProfileRepository.deleteAll();
        userRepository.deleteAll();

        citizenA = userRepository.save(new User("citizena@test.org", passwordEncoder.encode("Pass123!"), "Citizen A", "+111", "CITIZEN"));
        citizenB = userRepository.save(new User("citizenb@test.org", passwordEncoder.encode("Pass123!"), "Citizen B", "+222", "CITIZEN"));
        admin = userRepository.save(new User("admin@test.org", passwordEncoder.encode("Pass123!"), "Admin User", "+333", "ADMIN"));

        tokenCitizenA = tokenProvider.generateToken(UserPrincipal.create(citizenA));
        tokenCitizenB = tokenProvider.generateToken(UserPrincipal.create(citizenB));
        tokenAdmin = tokenProvider.generateToken(UserPrincipal.create(admin));
    }

    @Test
    @DisplayName("Citizen should submit complaint with photos and lat/lng coordinates")
    void shouldAllowCitizenToSubmitComplaint() throws Exception {
        CreateComplaintRequest request = new CreateComplaintRequest(
                "Plastic bottles at Central Park",
                "High accumulation of single-use plastic bottles near pond.",
                "PLASTIC_BOTTLES",
                "LARGE_DUMP",
                new BigDecimal("12.9715987"),
                new BigDecimal("77.5945627"),
                "Central Park, Sector 4",
                List.of("https://res.cloudinary.com/demo/image/upload/v123456/park_dump.jpg")
        );

        mockMvc.perform(post("/api/v1/complaints")
                        .header("Authorization", "Bearer " + tokenCitizenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.citizenId").value(citizenA.getId().toString()))
                .andExpect(jsonPath("$.images", hasSize(1)))
                .andExpect(jsonPath("$.images[0].imageUrl", containsString("park_dump.jpg")));

        List<ProgressUpdate> logs = progressUpdateRepository.findAll();
        assertFalse(logs.isEmpty());
        assertEquals("SUBMITTED", logs.get(0).getNewStatus());
    }

    @Test
    @DisplayName("Citizen should view own complaints list")
    void shouldAllowCitizenToViewOwnComplaints() throws Exception {
        Complaint c = new Complaint();
        c.setCitizen(citizenA);
        c.setTitle("Beach Plastic Waste");
        c.setDescription("Bottles washed ashore.");
        c.setLatitude(new BigDecimal("13.0000000"));
        c.setLongitude(new BigDecimal("80.0000000"));
        c.setStatus("SUBMITTED");
        complaintRepository.save(c);

        mockMvc.perform(get("/api/v1/complaints/my")
                        .header("Authorization", "Bearer " + tokenCitizenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Beach Plastic Waste"));
    }

    @Test
    @DisplayName("Resource Ownership: Citizen B cannot view Citizen A's complaint (HTTP 403)")
    void shouldPreventNonOwnerCitizenFromViewingOthersComplaint() throws Exception {
        Complaint c = new Complaint();
        c.setCitizen(citizenA);
        c.setTitle("Private Citizen A Complaint");
        c.setDescription("Details");
        c.setLatitude(new BigDecimal("13.0000000"));
        c.setLongitude(new BigDecimal("80.0000000"));
        c.setStatus("SUBMITTED");
        Complaint saved = complaintRepository.save(c);

        mockMvc.perform(get("/api/v1/complaints/" + saved.getId())
                        .header("Authorization", "Bearer " + tokenCitizenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("Admin should triage (UNDER_REVIEW) and verify complaint")
    void shouldAllowAdminToTriageAndVerifyComplaint() throws Exception {
        Complaint c = new Complaint();
        c.setCitizen(citizenA);
        c.setTitle("Street Waste Dump");
        c.setDescription("Details");
        c.setLatitude(new BigDecimal("13.0000000"));
        c.setLongitude(new BigDecimal("80.0000000"));
        c.setStatus("SUBMITTED");
        Complaint saved = complaintRepository.save(c);

        // 1. Triage -> UNDER_REVIEW
        mockMvc.perform(patch("/api/v1/complaints/" + saved.getId() + "/review")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .param("remarks", "Assigning inspector"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        // 2. Verify -> VERIFIED
        mockMvc.perform(patch("/api/v1/complaints/" + saved.getId() + "/verify")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .param("remarks", "Site confirmed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));
    }

    @Test
    @DisplayName("Admin should reject invalid complaint with mandatory rejection reason")
    void shouldAllowAdminToRejectComplaintWithReason() throws Exception {
        Complaint c = new Complaint();
        c.setCitizen(citizenA);
        c.setTitle("Invalid Spam Report");
        c.setDescription("Details");
        c.setLatitude(new BigDecimal("13.0000000"));
        c.setLongitude(new BigDecimal("80.0000000"));
        c.setStatus("SUBMITTED");
        Complaint saved = complaintRepository.save(c);

        ComplaintRejectRequest rejectReq = new ComplaintRejectRequest("Image does not show plastic waste.");

        mockMvc.perform(patch("/api/v1/complaints/" + saved.getId() + "/reject")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Image does not show plastic waste."));
    }

    @Test
    @DisplayName("Citizen owner can cancel complaint while SUBMITTED")
    void shouldAllowCitizenToCancelSubmittedComplaint() throws Exception {
        Complaint c = new Complaint();
        c.setCitizen(citizenA);
        c.setTitle("Reported by mistake");
        c.setDescription("Details");
        c.setLatitude(new BigDecimal("13.0000000"));
        c.setLongitude(new BigDecimal("80.0000000"));
        c.setStatus("SUBMITTED");
        Complaint saved = complaintRepository.save(c);

        ComplaintCancelRequest cancelReq = new ComplaintCancelRequest("Already cleaned by local store owner.");

        mockMvc.perform(patch("/api/v1/complaints/" + saved.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenCitizenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Already cleaned by local store owner."));
    }

    @Test
    @DisplayName("State Safeguard: Citizen cannot cancel once complaint is already VERIFIED")
    void shouldPreventCitizenFromCancellingVerifiedComplaint() throws Exception {
        Complaint c = new Complaint();
        c.setCitizen(citizenA);
        c.setTitle("Verified Report");
        c.setDescription("Details");
        c.setLatitude(new BigDecimal("13.0000000"));
        c.setLongitude(new BigDecimal("80.0000000"));
        c.setStatus("VERIFIED");
        Complaint saved = complaintRepository.save(c);

        ComplaintCancelRequest cancelReq = new ComplaintCancelRequest("Trying to cancel late.");

        mockMvc.perform(patch("/api/v1/complaints/" + saved.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tokenCitizenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().is5xxServerError()); // Throws IllegalStateException
    }
}
