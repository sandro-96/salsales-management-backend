package com.example.sales.constant;

public enum WebSocketMessageType {
    // Auth
    EMAIL_VERIFIED,
    PASSWORD_RESET,

    // Notifications (in-app bell)
    NOTIFICATION,

    // Order lifecycle (per-branch topic)
    ORDER_CREATED,
    ORDER_UPDATED,
    ORDER_STATUS_CHANGED,
    ORDER_DELETED,

    // Table lifecycle (per-branch topic)
    TABLE_CREATED,
    TABLE_UPDATED,
    TABLE_DELETED,
    TABLE_STATUS_CHANGED,
    TABLE_ASSIGNED,

    // Payment (per-branch topic; đi kèm ORDER_STATUS_CHANGED tương ứng)
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED
}
