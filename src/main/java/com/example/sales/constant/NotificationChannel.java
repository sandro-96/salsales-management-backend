package com.example.sales.constant;

/**
 * Kênh phân phối notification.
 *
 * Một NotificationType có thể được route tới 1 hoặc nhiều channel.
 * Dispatcher sẽ gọi sender tương ứng cho từng channel được bật.
 */
public enum NotificationChannel {
    IN_APP,
    EMAIL,
    SMS,
    PUSH
}
