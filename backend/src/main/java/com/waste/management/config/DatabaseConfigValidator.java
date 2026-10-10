package com.waste.management.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

public class DatabaseConfigValidator implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Skip validation when running automated test profile
        if (environment.acceptsProfiles(Profiles.of("test"))) {
            return;
        }

        String dbPassword = environment.getProperty("DB_PASSWORD");
        if (dbPassword == null || dbPassword.trim().isEmpty()) {
            String dbUrl = environment.getProperty("DB_URL", "jdbc:postgresql://localhost:5432/plastic_waste_db");
            String dbUser = environment.getProperty("DB_USERNAME", "postgres");

            throw new IllegalStateException(String.format(
                """
                
                ========================================================================================
                APPLICATION STARTUP FAILED - MISSING REQUIRED DATABASE CREDENTIALS
                ========================================================================================
                Reason: The 'DB_PASSWORD' environment variable is not defined or is empty.
                Target Database: %s
                Target Username: %s
                
                Security Notice:
                In accordance with security requirements, a default database password is NEVER provided.
                
                How to resolve:
                1. Set the environment variable in your terminal:
                   $env:DB_PASSWORD="your_postgres_password"
                2. Or define it in your backend environment profile before launching.
                ========================================================================================
                """,
                dbUrl,
                dbUser
            ));
        }
    }
}
