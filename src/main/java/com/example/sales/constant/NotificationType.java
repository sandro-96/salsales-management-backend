package com.example.sales.constant;

public enum NotificationType {
    // Order
    ORDER_CREATED,
    ORDER_UPDATED,
    ORDER_PAID,

    // Staff
    STAFF_ADDED,
    STAFF_REMOVED,

    // Support tickets
    TICKET_CREATED,
    TICKET_REPLIED,
    TICKET_STATUS_CHANGED,

    // Billing / Subscription
    BILLING_PLAN_UPGRADED,
    BILLING_PLAN_DOWNGRADED,
    BILLING_PLAN_EXPIRING_SOON,
    BILLING_PLAN_EXPIRED,
    BILLING_INVOICE_CREATED,
    BILLING_PAYMENT_SUCCESS,
    BILLING_PAYMENT_FAILED,

    // Generic
    SYSTEM,
    BROADCAST
}
