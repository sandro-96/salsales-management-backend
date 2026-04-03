package com.example.sales.constant;

public enum PointTransactionType {
    EARN,       // Tích điểm từ đơn hàng
    REDEEM,     // Đổi điểm (trừ vào đơn hàng)
    ADJUST,     // Điều chỉnh thủ công
    EXPIRE,     // Hết hạn
    REFUND      // Hoàn điểm khi hủy đơn
}
