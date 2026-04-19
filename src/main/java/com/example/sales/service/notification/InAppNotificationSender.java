package com.example.sales.service.notification;

import com.example.sales.constant.NotificationChannel;
import com.example.sales.dto.notification.NotificationEnvelope;
import com.example.sales.model.User;
import com.example.sales.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Channel IN_APP — ủy thác cho {@link NotificationService} hiện có
 * (persist Notification + push WebSocket). Đây chỉ là adapter; không
 * sao chép logic để đảm bảo backward compatibility với code cũ.
 */
@Component
@RequiredArgsConstructor
public class InAppNotificationSender implements NotificationSender {

    private final NotificationService notificationService;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void send(NotificationEnvelope envelope, User recipient, NotificationRouter.ChannelPlan plan) {
        notificationService.send(
                envelope.getShopId(),
                recipient.getId(),
                envelope.getType(),
                envelope.getTitle(),
                envelope.getMessage(),
                envelope.getReferenceId(),
                envelope.getReferenceType(),
                envelope.getActorId(),
                envelope.getActorName());
    }
}
