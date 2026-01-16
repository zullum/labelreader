package com.labelreader.service;

import com.labelreader.dto.NotificationDto;
import com.labelreader.entity.Notification;
import com.labelreader.entity.User;
import com.labelreader.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public Page<NotificationDto> getUserNotifications(User user, Boolean unreadOnly, Pageable pageable) {
        Page<Notification> notifications;
        if (unreadOnly != null && unreadOnly) {
            notifications = notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, false, pageable);
        } else {
            notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        }
        return notifications.map(NotificationDto::fromEntity);
    }

    public Long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsRead(user, false);
    }

    public NotificationDto markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        notification.setIsRead(true);
        notification = notificationRepository.save(notification);
        return NotificationDto.fromEntity(notification);
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsReadForUser(user);
    }

    public void deleteNotification(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        notificationRepository.delete(notification);
    }

    public void createNotification(User user, Notification.NotificationType type, String title, String message, String linkUrl) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .linkUrl(linkUrl)
                .isRead(false)
                .build();

        notificationRepository.save(notification);

        // Send email notification for important events
        if (type == Notification.NotificationType.NEW_RATING ||
            type == Notification.NotificationType.SIGNING_REQUEST ||
            type == Notification.NotificationType.REQUEST_RESPONSE) {
            emailService.sendNotificationEmail(user.getEmail(), title, message, linkUrl);
        }
    }
}
