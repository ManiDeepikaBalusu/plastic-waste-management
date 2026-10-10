package com.waste.management.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final DataSource dataSource;

    @Autowired
    public HealthController(@Autowired(required = false) DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "plastic-waste-backend");
        response.put("version", "1.0.0-SNAPSHOT");
        response.put("timestamp", Instant.now().toString());
        response.put("javaVersion", System.getProperty("java.version"));

        // Database Connectivity Health Check
        Map<String, Object> dbHealth = new LinkedHashMap<>();
        if (dataSource != null) {
            try (Connection conn = dataSource.getConnection()) {
                dbHealth.put("status", "UP");
                dbHealth.put("databaseProductName", conn.getMetaData().getDatabaseProductName());
                dbHealth.put("databaseProductVersion", conn.getMetaData().getDatabaseProductVersion());
            } catch (Exception ex) {
                dbHealth.put("status", "DOWN");
                dbHealth.put("error", ex.getMessage());
            }
        } else {
            dbHealth.put("status", "NOT_CONFIGURED");
        }
        response.put("database", dbHealth);
        response.put("message", "Plastic Waste Management Backend API is operational.");

        return ResponseEntity.ok(response);
    }
}
