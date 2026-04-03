package com.example.sales.dto.notification;

import com.example.sales.constant.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private String id;
    private String shopId;
    private String userId;

    private NotificationType type;
    private String title;
    private String message;
    private boolean read;

    private String referenceId;
    private String referenceType;

    private String actorId;
    private String actorName;

    private LocalDateTime createdAt;
}
