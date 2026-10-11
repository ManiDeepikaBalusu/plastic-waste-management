package com.waste.management.service;

import com.waste.management.dto.TokenValidationResponse;
import com.waste.management.entity.PasswordResetToken;
import com.waste.management.entity.User;
import com.waste.management.exception.InvalidTokenException;
import com.waste.management.repository.PasswordResetTokenRepository;
import com.waste.management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static final String GENERIC_RESET_REQUEST_MESSAGE =
            "If an account with that email exists, password reset instructions have been sent.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RateLimiterService rateLimiterService;

    private final long tokenExpirationMinutes;
    private final String frontendResetUrl;
    private final boolean devMode;

    // Secure in-memory holder for dev/automated tests ONLY (active only when devMode = true)
    private final Map<String, String> devTokenCache = new ConcurrentHashMap<>();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            RateLimiterService rateLimiterService,
            @Value("${app.password-reset.token-expiration-minutes:30}") long tokenExpirationMinutes,
            @Value("${app.password-reset.frontend-reset-url:http://localhost:5173/reset-password}") String frontendResetUrl,
            @Value("${app.password-reset.dev-mode:false}") boolean devMode) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.rateLimiterService = rateLimiterService;
        this.tokenExpirationMinutes = tokenExpirationMinutes;
        this.frontendResetUrl = frontendResetUrl;
        this.devMode = devMode;
    }

    /**
     * Initiates a password reset request.
     * Enforces rate limiting and anti-enumeration masking.
     */
    @Transactional
    public String requestPasswordReset(String email, String clientIp) {
        // 1. Rate limiting check per email and IP
        rateLimiterService.checkRateLimit(email, clientIp);

        if (!StringUtils.hasText(email)) {
            return GENERIC_RESET_REQUEST_MESSAGE;
        }

        String normalizedEmail = email.toLowerCase().trim();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Invalidate any previous unused tokens for this user
            tokenRepository.invalidateAllUnusedTokensForUser(user.getId(), OffsetDateTime.now());

            // Generate cryptographically random raw token (32 bytes = 64 hex chars)
            byte[] randomBytes = new byte[32];
            SECURE_RANDOM.nextBytes(randomBytes);
            String rawToken = HexFormat.of().formatHex(randomBytes);

            // Compute SHA-256 hash for secure database persistence
            String tokenHash = hashToken(rawToken);

            OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(tokenExpirationMinutes);
            PasswordResetToken resetToken = new PasswordResetToken(user, tokenHash, expiresAt);
            tokenRepository.save(resetToken);

            // Construct link and send email
            String resetUrl = frontendResetUrl + "?token=" + rawToken;
            emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);

            // Store in dev-mode cache for test assertions if enabled
            if (devMode) {
                devTokenCache.put(normalizedEmail, rawToken);
            }

            logger.info("[AUTH_SECURITY] Password reset token generated and recorded for user ID: {}", user.getId());
        } else {
            // Anti-enumeration: Account does not exist, do not disclose
            logger.info("[AUTH_SECURITY] Password reset requested for unregistered identifier. No action taken.");
        }

        return GENERIC_RESET_REQUEST_MESSAGE;
    }

    /**
     * Validates a password reset token.
     */
    @Transactional(readOnly = true)
    public TokenValidationResponse validateToken(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return TokenValidationResponse.invalid("Password reset token is missing.");
        }

        String tokenHash = hashToken(rawToken);
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isEmpty()) {
            return TokenValidationResponse.invalid("This password reset token is invalid.");
        }

        PasswordResetToken token = tokenOpt.get();
        if (Boolean.TRUE.equals(token.getIsUsed())) {
            return TokenValidationResponse.invalid("This password reset token has already been used.");
        }

        if (token.isExpired()) {
            return TokenValidationResponse.invalid("This password reset token has expired.");
        }

        String maskedEmail = maskEmail(token.getUser().getEmail());
        return TokenValidationResponse.valid(maskedEmail);
    }

    /**
     * Atomically validates token, updates password, and consumes token under a database lock.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (!StringUtils.hasText(rawToken)) {
            throw new InvalidTokenException("Password reset token is required.");
        }

        if (!StringUtils.hasText(newPassword) || newPassword.length() < 8) {
            throw new InvalidTokenException("New password must be at least 8 characters long.");
        }

        String tokenHash = hashToken(rawToken);

        // Fetch token with pessimistic write lock (SELECT ... FOR UPDATE) to prevent race conditions
        PasswordResetToken token = tokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token."));

        if (Boolean.TRUE.equals(token.getIsUsed())) {
            throw new InvalidTokenException("This password reset token has already been used.");
        }

        if (token.isExpired()) {
            throw new InvalidTokenException("This password reset token has expired.");
        }

        OffsetDateTime now = OffsetDateTime.now();

        // 1. Mark token consumed atomically
        token.setIsUsed(true);
        token.setUsedAt(now);
        tokenRepository.save(token);

        // 2. Update user's password with BCrypt encoder and update passwordChangedAt
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(now);
        userRepository.save(user);

        // 3. Invalidate any remaining unused tokens for this user
        tokenRepository.invalidateAllUnusedTokensForUser(user.getId(), now);

        if (devMode) {
            devTokenCache.remove(user.getEmail().toLowerCase().trim());
        }

        logger.info("[AUTH_SECURITY] Password successfully reset for user ID: {}. Previous sessions invalidated.", user.getId());
    }

    /**
     * Computes the SHA-256 digest of the token as a lowercase hex string.
     */
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in runtime", e);
        }
    }

    /**
     * Development/Testing-only hook to retrieve generated raw token without logging it.
     */
    public String getDevLatestToken(String email) {
        if (!devMode) {
            throw new IllegalStateException("Development testing hook is strictly disabled in production mode.");
        }
        return devTokenCache.get(email.toLowerCase().trim());
    }

    public void clearDevTokenCache() {
        devTokenCache.clear();
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];
        String maskedName = name.length() <= 2 ? name.charAt(0) + "***" : name.charAt(0) + "***" + name.charAt(name.length() - 1);
        return maskedName + "@" + domain;
    }
}
