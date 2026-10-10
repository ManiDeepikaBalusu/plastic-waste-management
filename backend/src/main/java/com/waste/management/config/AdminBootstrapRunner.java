package com.waste.management.config;

import com.waste.management.entity.User;
import com.waste.management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.initial-email:}")
    private String initialAdminEmail;

    @Value("${app.admin.initial-password:}")
    private String initialAdminPassword;

    @Value("${app.admin.initial-name:System Administrator}")
    private String initialAdminName;

    public AdminBootstrapRunner(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Only run if both admin email and password are provided in environment
        if (initialAdminEmail == null || initialAdminEmail.isBlank() ||
            initialAdminPassword == null || initialAdminPassword.isBlank()) {
            return;
        }

        String email = initialAdminEmail.trim().toLowerCase();

        // Check if user already exists
        if (userRepository.existsByEmail(email)) {
            logger.info("Admin bootstrap: Account with email '{}' already exists. Skipping bootstrap.", email);
            return;
        }

        // Create the initial administrator
        String hashedPassword = passwordEncoder.encode(initialAdminPassword);
        User adminUser = new User(email, hashedPassword, initialAdminName, null, "ADMIN");
        userRepository.save(adminUser);

        logger.info("====================================================================");
        logger.info("ADMIN BOOTSTRAP: Successfully created initial ADMIN account '{}'", email);
        logger.info("====================================================================");
    }
}
