package com.waste.management.controller;

import com.waste.management.entity.Notification;
import com.waste.management.security.UserPrincipal;
import com.waste.management.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Notification> notifications = notificationService.getUserNotifications(currentUser.getId());
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        notificationService.markAsRead(id, currentUser.getId());
        return ResponseEntity.ok(Map.of("message", "Notification marked as read."));
    }
}
