package com.waste.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waste.management.dto.CreateAssignmentRequest;
import com.waste.management.dto.CreateComplaintRequest;
import com.waste.management.dto.VolunteerStatusRequest;
import com.waste.management.entity.Complaint;
import com.waste.management.entity.User;
import com.waste.management.entity.VolunteerProfile;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EndToEndWorkflowIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VolunteerProfileRepository volunteerProfileRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

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
    private User volunteer;
    private User admin;

    private String tokenCitizenA;
    private String tokenCitizenB;
    private String tokenVolunteer;
    private String tokenAdmin;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        assignmentRepository.deleteAll();
        progressUpdateRepository.deleteAll();
        complaintImageRepository.deleteAll();
        complaintRepository.deleteAll();
        volunteerProfileRepository.deleteAll();
        userRepository.deleteAll();

        citizenA = userRepository.save(new User("citizen_a@test.org", passwordEncoder.encode("Pass123!"), "Citizen Alice", "+111", "CITIZEN"));
        citizenB = userRepository.save(new User("citizen_b@test.org", passwordEncoder.encode("Pass123!"), "Citizen Bob", "+222", "CITIZEN"));
        volunteer = userRepository.save(new User("volunteer@test.org", passwordEncoder.encode("Pass123!"), "Volunteer Vic", "+333", "VOLUNTEER"));
        volunteerProfileRepository.save(new VolunteerProfile(volunteer));

        admin = userRepository.save(new User("admin@test.org", passwordEncoder.encode("Pass123!"), "Admin Arthur", "+444", "ADMIN"));

        tokenCitizenA = tokenProvider.generateToken(UserPrincipal.create(citizenA));
        tokenCitizenB = tokenProvider.generateToken(UserPrincipal.create(citizenB));
        tokenVolunteer = tokenProvider.generateToken(UserPrincipal.create(volunteer));
        tokenAdmin = tokenProvider.generateToken(UserPrincipal.create(admin));
    }

    @Test
    @DisplayName("End-to-End Workflow: Citizen Report -> Admin Verify & Dispatch -> Volunteer Accept, Start & Resolve")
    void shouldExecuteFullCitizenToAdminToVolunteerWorkflow() throws Exception {
        // Step 1: Citizen A submits complaint with photo
        CreateComplaintRequest complaintReq = new CreateComplaintRequest(
                "Lakeside Plastic Bottle Accumulation",
                "Severe pile of plastic bottles and containers along lakeside promenade.",
                "PLASTIC_BOTTLES",
                "LARGE_DUMP",
                new BigDecimal("12.971598"),
                new BigDecimal("77.594562"),
                "Central Lake Park Gate 2",
                List.of()
        );

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(complaintReq)
        );

        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01};
        MockMultipartFile initialPhoto = new MockMultipartFile(
                "files",
                "lake_dump.jpg",
                "image/jpeg",
                jpegBytes
        );

        MvcResult submitResult = mockMvc.perform(multipart("/api/v1/complaints/submit-with-photos")
                        .file(dataPart)
                        .file(initialPhoto)
                        .header("Authorization", "Bearer " + tokenCitizenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.citizenId").value(citizenA.getId().toString()))
                .andReturn();

        String complaintIdStr = objectMapper.readTree(submitResult.getResponse().getContentAsString()).get("id").asText();
        UUID complaintId = UUID.fromString(complaintIdStr);

        // Step 2: Citizen B attempts to access or cancel Citizen A's complaint -> Expect 403 Forbidden
        mockMvc.perform(get("/api/v1/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + tokenCitizenB))
                .andExpect(status().isForbidden());

        // Step 3: Admin triages (UNDER_REVIEW) and verifies complaint (VERIFIED)
        mockMvc.perform(patch("/api/v1/complaints/" + complaintId + "/review")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .param("remarks", "Reviewing lakeside site report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        mockMvc.perform(patch("/api/v1/complaints/" + complaintId + "/verify")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .param("remarks", "Site confirmed. Queued for volunteer dispatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));

        // Step 4: Volunteer toggles online availability
        VolunteerStatusRequest statusReq = new VolunteerStatusRequest(
                true,
                new BigDecimal("12.972000"),
                new BigDecimal("77.595000")
        );

        mockMvc.perform(put("/api/v1/volunteer/status")
                        .header("Authorization", "Bearer " + tokenVolunteer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAvailable").value(true));

        // Step 5: Admin dispatches complaint to Volunteer
        CreateAssignmentRequest assignReq = new CreateAssignmentRequest(
                complaintId,
                volunteer.getId(),
                "Please bring heavy duty bags and safety gloves."
        );

        MvcResult assignResult = mockMvc.perform(post("/api/v1/assignments")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.volunteerId").value(volunteer.getId().toString()))
                .andReturn();

        String assignmentIdStr = objectMapper.readTree(assignResult.getResponse().getContentAsString()).get("id").asText();
        UUID assignmentId = UUID.fromString(assignmentIdStr);

        // Verify Complaint is in ASSIGNED state
        mockMvc.perform(get("/api/v1/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        // Step 6: Volunteer accepts task
        mockMvc.perform(patch("/api/v1/assignments/" + assignmentId + "/accept")
                        .header("Authorization", "Bearer " + tokenVolunteer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Step 7: Volunteer starts work (IN_PROGRESS)
        mockMvc.perform(patch("/api/v1/assignments/" + assignmentId + "/start")
                        .header("Authorization", "Bearer " + tokenVolunteer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Step 8: Volunteer uploads resolution photo and marks COMPLETED / RESOLVED
        MockMultipartFile resolutionPhoto = new MockMultipartFile(
                "files",
                "clean_lake.jpg",
                "image/jpeg",
                jpegBytes
        );

        mockMvc.perform(multipart("/api/v1/assignments/" + assignmentId + "/resolve")
                        .file(resolutionPhoto)
                        .param("notes", "Collected 40kg of PET plastic. Transported to recycling center.")
                        .header("Authorization", "Bearer " + tokenVolunteer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Verify Complaint is now RESOLVED
        mockMvc.perform(get("/api/v1/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + tokenCitizenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // Verify Volunteer Profile totalResolved incremented
        VolunteerProfile updatedProfile = volunteerProfileRepository.findByUserId(volunteer.getId()).orElseThrow();
        assertEquals(1, updatedProfile.getTotalResolved());
    }
}
