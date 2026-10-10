package com.waste.management.repository;

import com.waste.management.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RepositoryIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VolunteerProfileRepository volunteerProfileRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintImageRepository complaintImageRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private ProgressUpdateRepository progressUpdateRepository;

    @Autowired
    private VerificationResultRepository verificationResultRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("Should persist and retrieve User with BCrypt hashed password")
    void shouldPersistAndRetrieveUser() {
        // Passwords must be stored as secure hashes, never plain text
        String secureBcryptHash = "$2a$12$e80yqR3kG.C3t9w1pCg1xe5nI9D7hX2aL8.3z.q0L1V6p7o9s3R8i";
        User user = new User("citizen@example.com", secureBcryptHash, "Jane Citizen", "+1234567890", "CITIZEN");

        User saved = userRepository.save(user);
        assertNotNull(saved.getId());
        assertEquals("citizen@example.com", saved.getEmail());

        Optional<User> found = userRepository.findByEmail("citizen@example.com");
        assertTrue(found.isPresent());
        assertEquals(secureBcryptHash, found.get().getPasswordHash());
    }

    @Test
    @DisplayName("Should persist VolunteerProfile and query available volunteers")
    void shouldPersistVolunteerProfile() {
        User volunteerUser = new User("volunteer@example.com", "hash", "Bob Volunteer", "+1234567891", "VOLUNTEER");
        userRepository.save(volunteerUser);

        VolunteerProfile profile = new VolunteerProfile(volunteerUser);
        profile.setIsAvailable(true);
        profile.setCurrentLatitude(new BigDecimal("12.9715987"));
        profile.setCurrentLongitude(new BigDecimal("77.5945627"));

        volunteerProfileRepository.save(profile);

        List<VolunteerProfile> available = volunteerProfileRepository.findByIsAvailableTrue();
        assertFalse(available.isEmpty());
        assertEquals(new BigDecimal("12.9715987"), available.get(0).getCurrentLatitude());
    }

    @Test
    @DisplayName("Should persist Complaint with Cloudinary image URLs and audit history")
    void shouldPersistComplaintWithImageAndAudit() {
        User citizen = new User("reporter@example.com", "hash", "Reporter", "+111", "CITIZEN");
        User admin = new User("admin@example.com", "hash", "Admin", "+222", "ADMIN");
        userRepository.save(citizen);
        userRepository.save(admin);

        Complaint complaint = new Complaint();
        complaint.setCitizen(citizen);
        complaint.setTitle("Illegal plastic dump near lake");
        complaint.setDescription("Over 50 plastic bottles and bags dumped near lake shore.");
        complaint.setLatitude(new BigDecimal("12.9352000"));
        complaint.setLongitude(new BigDecimal("77.6245000"));
        complaint.setWasteType("PLASTIC_BOTTLES");
        complaint.setStatus("SUBMITTED");

        Complaint savedComplaint = complaintRepository.save(complaint);
        assertNotNull(savedComplaint.getId());

        // Store Cloudinary Image URL (No binary storage in database)
        ComplaintImage img = new ComplaintImage(
            savedComplaint,
            "https://res.cloudinary.com/demo/image/upload/v123456789/waste_initial_01.jpg",
            "waste_initial_01",
            "INITIAL_PROOF",
            citizen
        );
        complaintImageRepository.save(img);

        List<ComplaintImage> images = complaintImageRepository.findByComplaintId(savedComplaint.getId());
        assertEquals(1, images.size());
        assertEquals("INITIAL_PROOF", images.get(0).getImageType());

        // Record progress update audit log
        ProgressUpdate log = new ProgressUpdate(
            savedComplaint,
            admin,
            "SUBMITTED",
            "UNDER_REVIEW",
            "STATUS_CHANGE",
            "Admin assigned for initial evaluation"
        );
        progressUpdateRepository.save(log);

        List<ProgressUpdate> updates = progressUpdateRepository.findByComplaintIdOrderByCreatedAtDesc(savedComplaint.getId());
        assertEquals(1, updates.size());
        assertEquals("UNDER_REVIEW", updates.get(0).getNewStatus());
    }
}
