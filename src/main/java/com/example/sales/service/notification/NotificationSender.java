package com.example.sales.service.notification;

import com.example.sales.constant.NotificationChannel;
import com.example.sales.dto.notification.NotificationEnvelope;
import com.example.sales.model.User;

/**
 * Strategy cho một channel cụ thể (IN_APP / EMAIL / SMS / PUSH).
 * Dispatcher sẽ iterate các sender được bật cho NotificationType và
 * gọi {@link #send(NotificationEnvelope, User, NotificationRouter.ChannelPlan)}.
 */
public interface NotificationSender {

    NotificationChannel channel();

    /**
     * @param envelope dữ liệu producer đã chuẩn bị
     * @param recipient user nhận (đã được resolve sẵn, đảm bảo không null)
     * @param plan      cấu hình channel (template, subject, v.v.) từ router
     */
    void send(NotificationEnvelope envelope, User recipient, NotificationRouter.ChannelPlan plan);
}
