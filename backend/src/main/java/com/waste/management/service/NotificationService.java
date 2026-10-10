package com.waste.management.service;

import com.waste.management.entity.Notification;
import com.waste.management.entity.User;
import com.waste.management.exception.ResourceNotFoundException;
import com.waste.management.exception.UnauthorizedActionException;
import com.waste.management.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification createNotification(User user, String title, String message, String type, UUID referenceId) {
        Notification notification = new Notification(user, title, message, type, referenceId);
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("You are not authorized to update this notification.");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
}
