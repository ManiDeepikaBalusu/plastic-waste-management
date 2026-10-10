package com.waste.management.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waste.management.dto.LoginRequest;
import com.waste.management.dto.RegisterRequest;
import com.waste.management.entity.User;
import com.waste.management.repository.UserRepository;
import com.waste.management.repository.VolunteerProfileRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VolunteerProfileRepository volunteerProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        volunteerProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully register as CITIZEN and receive HttpOnly cookie")
    void shouldRegisterCitizenSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "citizen@greenwaste.org",
                "SecurePassword123!",
                "Citizen Alex",
                "+1234567890",
                "CITIZEN"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("citizen@greenwaste.org"))
                .andExpect(jsonPath("$.user.role").value("CITIZEN"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist()) // Never expose password hash!
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    @DisplayName("Should register VOLUNTEER and auto-provision volunteer profile")
    void shouldRegisterVolunteerSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "volunteer@greenwaste.org",
                "SecureVolunteer123!",
                "Volunteer Sam",
                "+1987654321",
                "VOLUNTEER"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("VOLUNTEER"));

        User savedUser = userRepository.findByEmail("volunteer@greenwaste.org").orElseThrow();
        assertTrue(volunteerProfileRepository.findByUserId(savedUser.getId()).isPresent());
    }

    @Test
    @DisplayName("Should strictly block public self-registration as ADMIN (Security Guard)")
    void shouldRejectPublicAdminRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "hacker@admin.org",
                "HackerPassword123!",
                "Fake Admin",
                "+1112223333",
                "ADMIN"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("Public self-registration as ADMIN is strictly prohibited")));
    }

    @Test
    @DisplayName("Should login successfully with valid credentials and return JWT & HttpOnly cookie")
    void shouldLoginSuccessfully() throws Exception {
        // Seed user with BCrypt hash
        User user = new User("valid@user.org", passwordEncoder.encode("ValidPassword123!"), "Valid User", "+123", "CITIZEN");
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("valid@user.org", "ValidPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("valid@user.org"));
    }

    @Test
    @DisplayName("Should fail login with invalid password (HTTP 401)")
    void shouldFailLoginWithInvalidPassword() throws Exception {
        User user = new User("valid@user.org", passwordEncoder.encode("ValidPassword123!"), "Valid User", "+123", "CITIZEN");
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("valid@user.org", "WrongPassword999!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Should fail login with non-existent email (HTTP 401)")
    void shouldFailLoginWithNonExistentEmail() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nobody@nowhere.org", "AnyPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Should access /users/me with valid Bearer token")
    void shouldAccessProtectedMeEndpointWithToken() throws Exception {
        User user = new User("me@user.org", passwordEncoder.encode("Password123!"), "Me User", "+123", "CITIZEN");
        User saved = userRepository.save(user);

        String token = tokenProvider.generateToken(UserPrincipal.create(saved));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@user.org"))
                .andExpect(jsonPath("$.id").value(saved.getId().toString()));
    }

    @Test
    @DisplayName("Should reject unauthenticated access to protected endpoint (HTTP 401)")
    void shouldRejectUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Should reject CITIZEN accessing ADMIN endpoint (HTTP 403 Forbidden)")
    void shouldRejectCitizenAccessingAdminEndpoint() throws Exception {
        User citizen = new User("citizen@test.org", passwordEncoder.encode("Pass123!"), "Citizen User", "+123", "CITIZEN");
        User savedCitizen = userRepository.save(citizen);

        String citizenToken = tokenProvider.generateToken(UserPrincipal.create(savedCitizen));

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("Should allow ADMIN accessing ADMIN endpoint (HTTP 200)")
    void shouldAllowAdminAccessingAdminEndpoint() throws Exception {
        User admin = new User("admin@test.org", passwordEncoder.encode("Pass123!"), "Admin User", "+123", "ADMIN");
        User savedAdmin = userRepository.save(admin);

        String adminToken = tokenProvider.generateToken(UserPrincipal.create(savedAdmin));

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Resource Ownership: Block user from viewing another user's personal profile (HTTP 403)")
    void shouldDenyAccessToAnotherUsersData() throws Exception {
        User userA = new User("userA@test.org", passwordEncoder.encode("Pass123!"), "User A", "+123", "CITIZEN");
        User userB = new User("userB@test.org", passwordEncoder.encode("Pass123!"), "User B", "+123", "CITIZEN");
        User savedUserA = userRepository.save(userA);
        User savedUserB = userRepository.save(userB);

        String tokenUserA = tokenProvider.generateToken(UserPrincipal.create(savedUserA));

        // User A tries to view User B's details -> Must be blocked!
        mockMvc.perform(get("/api/v1/users/" + savedUserB.getId())
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message", containsString("You are not authorized to view another user's profile")));
    }

    @Test
    @DisplayName("Resource Ownership: Allow user to view their own profile (HTTP 200)")
    void shouldAllowAccessToOwnUserData() throws Exception {
        User userA = new User("userA@test.org", passwordEncoder.encode("Pass123!"), "User A", "+123", "CITIZEN");
        User savedUserA = userRepository.save(userA);

        String tokenUserA = tokenProvider.generateToken(UserPrincipal.create(savedUserA));

        mockMvc.perform(get("/api/v1/users/" + savedUserA.getId())
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("userA@test.org"));
    }

    @Test
    @DisplayName("Resource Ownership: Allow ADMIN to view any user's profile (HTTP 200)")
    void shouldAllowAdminAccessToAnyUserData() throws Exception {
        User citizen = new User("citizen@test.org", passwordEncoder.encode("Pass123!"), "Citizen User", "+123", "CITIZEN");
        User admin = new User("admin@test.org", passwordEncoder.encode("Pass123!"), "Admin User", "+123", "ADMIN");
        User savedCitizen = userRepository.save(citizen);
        User savedAdmin = userRepository.save(admin);

        String adminToken = tokenProvider.generateToken(UserPrincipal.create(savedAdmin));

        mockMvc.perform(get("/api/v1/users/" + savedCitizen.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("citizen@test.org"));
    }
}
