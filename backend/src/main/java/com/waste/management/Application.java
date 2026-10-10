package com.waste.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Application.class);
        
        // Fast-fail configuration validator for missing database credentials
        app.addInitializers(applicationContext -> {
            Environment env = applicationContext.getEnvironment();
            
            boolean isTest = false;
            for (String profile : env.getActiveProfiles()) {
                if ("test".equalsIgnoreCase(profile)) {
                    isTest = true;
                    break;
                }
            }

            if (!isTest) {
                String dbPassword = env.getProperty("DB_PASSWORD");
                if (dbPassword == null || dbPassword.trim().isEmpty()) {
                    String dbUrl = env.getProperty("DB_URL", "jdbc:postgresql://localhost:5432/plastic_waste_db");
                    String dbUser = env.getProperty("DB_USERNAME", "postgres");

                    throw new ApplicationContextException(
                        "\n========================================================================================\n" +
                        "CRITICAL CONFIGURATION ERROR: MISSING DATABASE CREDENTIALS\n" +
                        "========================================================================================\n" +
                        "Required environment variable 'DB_PASSWORD' is not set or is empty.\n" +
                        "Target Database URL  : " + dbUrl + "\n" +
                        "Target Database User : " + dbUser + "\n\n" +
                        "Security Policy: In accordance with security standards, database passwords are never\n" +
                        "hardcoded or defaulted.\n\n" +
                        "How to resolve:\n" +
                        "  PowerShell:  $env:DB_PASSWORD=\"your_postgres_password\"\n" +
                        "  Command Prompt: set DB_PASSWORD=your_postgres_password\n" +
                        "  Or define DB_PASSWORD in your backend environment profile.\n" +
                        "========================================================================================\n"
                    );
                }
            }
        });

        app.run(args);
    }
}
