package com.waste.management.controller;

import com.waste.management.dto.UserResponse;
import com.waste.management.entity.User;
import com.waste.management.exception.ResourceNotFoundException;
import com.waste.management.exception.UnauthorizedActionException;
import com.waste.management.repository.UserRepository;
import com.waste.management.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUser.getId()));

        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        // Resource Ownership Guard: Only ADMIN or the user themselves may access their record
        boolean isOwner = currentUser.getId().equals(id);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedActionException("Access Denied: You are not authorized to view another user's profile.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }
}
