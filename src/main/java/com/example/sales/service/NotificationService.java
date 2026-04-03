package com.example.sales.service;

import com.example.sales.constant.NotificationType;
import com.example.sales.constant.WebSocketMessageType;
import com.example.sales.dto.notification.NotificationResponse;
import com.example.sales.dto.websocket.WebSocketMessage;
import com.example.sales.model.Notification;
import com.example.sales.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Create and persist a notification, then push via WebSocket.
     * This is the main entry-point for other services to trigger notifications.
     */
    public void send(String shopId, String userId, NotificationType type,
                     String title, String message,
                     String referenceId, String referenceType,
                     String actorId, String actorName) {
        Notification notification = Notification.builder()
                .shopId(shopId)
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .read(false)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .actorId(actorId)
                .actorName(actorName)
                .build();

        notification = notificationRepository.save(notification);

        try {
            NotificationResponse response = toResponse(notification);
            messagingTemplate.convertAndSend(
                    "/topic/notifications/" + userId,
                    new WebSocketMessage<>(WebSocketMessageType.NOTIFICATION, response)
            );
        } catch (Exception e) {
            log.warn("Failed to push WebSocket notification to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Send a notification to multiple users at once (e.g. all managers of a shop).
     */
    public void sendToMultiple(String shopId, List<String> userIds, NotificationType type,
                               String title, String message,
                               String referenceId, String referenceType,
                               String actorId, String actorName) {
        for (String userId : userIds) {
            send(shopId, userId, type, title, message, referenceId, referenceType, actorId, actorName);
        }
    }

    public Page<NotificationResponse> getNotifications(String userId, String shopId,
                                                       String readFilter, Pageable pageable) {
        Page<Notification> page;

        if (readFilter != null && !readFilter.isBlank()) {
            boolean isRead = Boolean.parseBoolean(readFilter);
            if (shopId != null && !shopId.isBlank()) {
                page = notificationRepository.findByUserIdAndShopIdAndReadAndDeletedFalse(
                        userId, shopId, isRead, pageable);
            } else {
                page = notificationRepository.findByUserIdAndReadAndDeletedFalse(
                        userId, isRead, pageable);
            }
        } else {
            if (shopId != null && !shopId.isBlank()) {
                page = notificationRepository.findByUserIdAndShopIdAndDeletedFalse(
                        userId, shopId, pageable);
            } else {
                page = notificationRepository.findByUserIdAndDeletedFalse(userId, pageable);
            }
        }

        return page.map(this::toResponse);
    }

    public long getUnreadCount(String userId, String shopId) {
        if (shopId != null && !shopId.isBlank()) {
            return notificationRepository.countByUserIdAndShopIdAndReadFalseAndDeletedFalse(userId, shopId);
        }
        return notificationRepository.countByUserIdAndReadFalseAndDeletedFalse(userId);
    }

    public NotificationResponse markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findByIdAndUserIdAndDeletedFalse(notificationId, userId)
                .orElse(null);
        if (notification == null) return null;

        notification.setRead(true);
        notification = notificationRepository.save(notification);
        return toResponse(notification);
    }

    public void markAllAsRead(String userId, String shopId) {
        List<Notification> unread;
        if (shopId != null && !shopId.isBlank()) {
            unread = notificationRepository.findByUserIdAndShopIdAndReadFalseAndDeletedFalse(userId, shopId);
        } else {
            unread = notificationRepository.findByUserIdAndReadFalseAndDeletedFalse(userId);
        }

        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    public void deleteNotification(String notificationId, String userId) {
        Optional<Notification> opt = notificationRepository.findByIdAndUserIdAndDeletedFalse(notificationId, userId);
        opt.ifPresent(notification -> {
            notification.setDeleted(true);
            notification.setDeletedAt(LocalDateTime.now());
            notificationRepository.save(notification);
        });
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .shopId(n.getShopId())
                .userId(n.getUserId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .read(n.isRead())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .actorId(n.getActorId())
                .actorName(n.getActorName())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
