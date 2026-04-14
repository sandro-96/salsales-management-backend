package com.example.sales.constant;

/**
 * Trạng thái thanh toán độc lập với {@link OrderStatus}.
 */
public enum PaymentStatus {
    /** Chưa thu tiền (chưa chọn hoặc chờ xác nhận thanh toán ngay). */
    UNPAID,
    /** Đã thu đủ / đã xác nhận thanh toán. */
    PAID,
    /**
     * Đơn ghi nhận thu tiền khi giao (Ship COD, …) — chờ nhân viên xác nhận đã thu.
     */
    PENDING_COLLECTION
}
