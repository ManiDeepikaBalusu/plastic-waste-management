package com.waste.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waste.management.dto.CreateComplaintRequest;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.waste.management.entity.ComplaintImage;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComplaintPhotoSubmissionSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private VerificationResultRepository verificationResultRepository;

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

    private User citizen;
    private String tokenCitizen;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        progressUpdateRepository.deleteAll();
        verificationResultRepository.deleteAll();
        complaintImageRepository.deleteAll();
        complaintRepository.deleteAll();
        userRepository.deleteAll();

        citizen = userRepository.save(new User("photocitizen@test.org", passwordEncoder.encode("Pass123!"), "Photo Citizen", "+12345", "CITIZEN"));
        tokenCitizen = tokenProvider.generateToken(UserPrincipal.create(citizen));
    }

    @Test
    @DisplayName("Multipart Upload: Should accept valid JPEG with valid magic bytes")
    void shouldAcceptValidJpegUpload() throws Exception {
        CreateComplaintRequest request = new CreateComplaintRequest(
                "Plastic bottle pile near lake",
                "Large pile of PET bottles needing cleanup",
                "PLASTIC_BOTTLES",
                "LARGE_DUMP",
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                "Lake Road, Sector 2",
                List.of()
        );

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(request)
        );

        // Valid JPEG Magic Bytes: 0xFF, 0xD8, 0xFF, 0xE0 ...
        byte[] validJpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01};
        MockMultipartFile filePart = new MockMultipartFile(
                "files",
                "waste_photo.jpg",
                "image/jpeg",
                validJpegBytes
        );

        mockMvc.perform(multipart("/api/v1/complaints/submit-with-photos")
                        .file(dataPart)
                        .file(filePart)
                        .header("Authorization", "Bearer " + tokenCitizen))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.images", hasSize(1)));
    }

    @Test
    @DisplayName("Magic Byte Security: Reject file with fake .jpg extension but invalid magic bytes")
    void shouldRejectSpoofedFileExtension() throws Exception {
        CreateComplaintRequest request = new CreateComplaintRequest(
                "Fake image report",
                "Descr",
                "PLASTIC_BOTTLES",
                "SMALL",
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                "Address",
                List.of()
        );

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(request)
        );

        // Fake image content (text script disguised as JPG)
        byte[] fakeBytes = "<script>alert('hack')</script>".getBytes();
        MockMultipartFile spoofedFile = new MockMultipartFile(
                "files",
                "malicious.jpg",
                "image/jpeg",
                fakeBytes
        );

        mockMvc.perform(multipart("/api/v1/complaints/submit-with-photos")
                        .file(dataPart)
                        .file(spoofedFile)
                        .header("Authorization", "Bearer " + tokenCitizen))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("magic bytes")));
    }

    @Test
    @DisplayName("Validation: Reject submission with invalid latitude out of bounds (> 90.0)")
    void shouldRejectOutOfBoundsLatitude() throws Exception {
        CreateComplaintRequest request = new CreateComplaintRequest(
                "Invalid Lat Report",
                "Descr",
                "PLASTIC_BOTTLES",
                "SMALL",
                new BigDecimal("95.0000"), // Invalid lat > 90
                new BigDecimal("77.5946"),
                "Address",
                List.of()
        );

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(request)
        );

        byte[] validJpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01};
        MockMultipartFile filePart = new MockMultipartFile(
                "files",
                "waste_photo.jpg",
                "image/jpeg",
                validJpegBytes
        );

        mockMvc.perform(multipart("/api/v1/complaints/submit-with-photos")
                        .file(dataPart)
                        .file(filePart)
                        .header("Authorization", "Bearer " + tokenCitizen))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.latitude").value("Latitude must be less than or equal to 90.0"));
    }

    @Test
    @DisplayName("Security Check: Reject unauthenticated complaint submission (HTTP 401)")
    void shouldRejectUnauthenticatedRequest() throws Exception {
        CreateComplaintRequest request = new CreateComplaintRequest(
                "Unauth Report",
                "Descr",
                "PLASTIC_BOTTLES",
                "SMALL",
                new BigDecimal("12.9716"),
                new BigDecimal("77.5946"),
                "Address",
                List.of()
        );

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(request)
        );

        byte[] validJpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01};
        MockMultipartFile filePart = new MockMultipartFile(
                "files",
                "waste_photo.jpg",
                "image/jpeg",
                validJpegBytes
        );

        mockMvc.perform(multipart("/api/v1/complaints/submit-with-photos")
                        .file(dataPart)
                        .file(filePart))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Multipart Tolerant Mapping: Handle field-name mismatches (deviceLat, deviceLon, accuracy, capturedAt, lastModifiedAt)")
    void shouldAcceptMismatchedFieldNamesInMultipartJson() throws Exception {
        String jsonPayloadWithAliases = """
                {
                    "title": "Park Dump with Aliased GPS",
                    "description": "Plastic trash heap with aliased field names",
                    "wasteType": "PLASTIC_BOTTLES",
                    "estimatedSize": "MEDIUM_PILE",
                    "latitude": 12.971600,
                    "longitude": 77.594600,
                    "addressText": "Main Park, Sector 4",
                    "deviceLat": 12.971620,
                    "deviceLon": 77.594630,
                    "accuracy": 8.5,
                    "capturedAt": "2026-10-11T10:15:30Z",
                    "lastModifiedAt": "2026-10-11T10:14:00Z",
                    "source": "CAMERA"
                }
                """;

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                "application/json",
                jsonPayloadWithAliases.getBytes(StandardCharsets.UTF_8)
        );

        byte[] validJpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01};
        MockMultipartFile filePart = new MockMultipartFile(
                "files",
                "aliased_fields.jpg",
                "image/jpeg",
                validJpegBytes
        );

        mockMvc.perform(multipart("/api/v1/complaints/submit-with-photos")
                        .file(dataPart)
                        .file(filePart)
                        .header("Authorization", "Bearer " + tokenCitizen))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.images", hasSize(1)));

        // Verify database entity populated with aliased coordinates and accuracy
        List<ComplaintImage> images = complaintImageRepository.findAll();
        ComplaintImage savedImg = images.get(images.size() - 1);
        assertNotNull(savedImg.getDeviceLatitude(), "deviceLatitude should be mapped from deviceLat");
        assertEquals(new BigDecimal("12.9716200"), savedImg.getDeviceLatitude());
        assertEquals(new BigDecimal("77.5946300"), savedImg.getDeviceLongitude());
        assertEquals(8.5, savedImg.getDeviceGpsAccuracy());
        assertNotNull(savedImg.getDeviceCapturedAt());
        assertNotNull(savedImg.getFileLastModifiedAt());
    }

    @Test
    @DisplayName("Validation: Reject submission with invalid longitude out of bounds (> 180.0)")
    void shouldRejectOutOfBoundsLongitude() throws Exception {
        CreateComplaintRequest request = new CreateComplaintRequest(
                "Invalid Lng Report",
                "Descr",
                "PLASTIC_BOTTLES",
                "SMALL",
                new BigDecimal("12.9716"),
                new BigDecimal("185.0000"), // Invalid lng > 180
                "Address",
                List.of()
        );

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(request)
        );

        byte[] validJpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01};
        MockMultipartFile filePart = new MockMultipartFile(
                "files",
                "waste_photo.jpg",
                "image/jpeg",
                validJpegBytes
        );

        mockMvc.perform(multipart("/api/v1/complaints/submit-with-photos")
                        .file(dataPart)
                        .file(filePart)
                        .header("Authorization", "Bearer " + tokenCitizen))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.longitude").value("Longitude must be less than or equal to 180.0"));
    }
}
