package com.waste.management.service;

import com.waste.management.dto.AuthResponse;
import com.waste.management.dto.LoginRequest;
import com.waste.management.dto.RegisterRequest;
import com.waste.management.dto.UserResponse;
import com.waste.management.entity.User;
import com.waste.management.entity.VolunteerProfile;
import com.waste.management.exception.InvalidRoleException;
import com.waste.management.exception.UserAlreadyExistsException;
import com.waste.management.repository.UserRepository;
import com.waste.management.repository.VolunteerProfileRepository;
import com.waste.management.security.JwtTokenProvider;
import com.waste.management.security.UserPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthService(
            UserRepository userRepository,
            VolunteerProfileRepository volunteerProfileRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.volunteerProfileRepository = volunteerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        String email = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("A user with this email address already exists.");
        }

        String normalizedRole = request.role().toUpperCase().trim();

        // Security Guard: Prevent public self-registration as ADMIN
        if ("ADMIN".equals(normalizedRole)) {
            throw new InvalidRoleException("Public self-registration as ADMIN is strictly prohibited. Admin accounts can only be promoted by existing administrators.");
        }

        if (!"CITIZEN".equals(normalizedRole) && !"VOLUNTEER".equals(normalizedRole)) {
            throw new InvalidRoleException("Invalid role '" + request.role() + "'. Allowed registration roles are CITIZEN or VOLUNTEER.");
        }

        // Hash password securely with BCrypt
        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(
                email,
                passwordHash,
                request.fullName().trim(),
                request.phone() != null ? request.phone().trim() : null,
                normalizedRole
        );
        User savedUser = userRepository.save(user);

        // Auto-provision volunteer profile if registering as VOLUNTEER
        if ("VOLUNTEER".equals(normalizedRole)) {
            VolunteerProfile profile = new VolunteerProfile(savedUser);
            volunteerProfileRepository.save(profile);
        }

        UserPrincipal userPrincipal = UserPrincipal.create(savedUser);
        String token = tokenProvider.generateToken(userPrincipal);

        // Attach secure HttpOnly cookie for React SPA
        attachHttpOnlyCookie(response, token, tokenProvider.getJwtExpirationMs() / 1000);

        return new AuthResponse(
                token,
                "Bearer",
                tokenProvider.getJwtExpirationMs(),
                UserResponse.fromEntity(savedUser),
                "User successfully registered."
        );
    }

    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().toLowerCase().trim(),
                        request.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String token = tokenProvider.generateToken(userPrincipal);

        // Attach secure HttpOnly cookie for React SPA
        attachHttpOnlyCookie(response, token, tokenProvider.getJwtExpirationMs() / 1000);

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));

        return new AuthResponse(
                token,
                "Bearer",
                tokenProvider.getJwtExpirationMs(),
                UserResponse.fromEntity(user),
                "Login successful."
        );
    }

    public void logout(HttpServletResponse response) {
        // Clear HttpOnly cookie
        attachHttpOnlyCookie(response, "", 0);
        SecurityContextHolder.clearContext();
    }

    private void attachHttpOnlyCookie(HttpServletResponse response, String value, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(tokenProvider.getCookieName(), value)
                .httpOnly(true)
                .secure(false) // Set to true in production over HTTPS
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
