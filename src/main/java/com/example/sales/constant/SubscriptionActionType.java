// File: src/main/java/com/example/sales/constant/SubscriptionActionType.java
package com.example.sales.constant;

public enum SubscriptionActionType {
    UPGRADE,          // Người dùng nâng cấp gói (legacy)
    DOWNGRADE,        // Người dùng chủ động hạ gói (legacy)
    AUTO_DOWNGRADE,   // Hệ thống tự động hạ gói khi hết hạn (legacy)
    TRIAL_EXPIRED,    // Gói thử nghiệm kết thúc
    ADMIN_OVERRIDE,   // Admin đổi plan/status thủ công
    ADMIN_EXTEND,     // Admin gia hạn plan
    PAYMENT,          // Shop thanh toán thành công — mở chu kỳ mới (hoặc gia hạn)
    PAYMENT_FAILED,   // Ghi nhận thanh toán thất bại (webhook/gateway)
    PERIOD_EXPIRED,   // Hệ thống auto expire chu kỳ ACTIVE
    CANCELLED         // Shop/admin huỷ subscription
}
