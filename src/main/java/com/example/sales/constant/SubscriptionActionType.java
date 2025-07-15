// File: com/example/sales/constant/SubscriptionActionType.java
package com.example.sales.constant;

public enum SubscriptionActionType {
    UPGRADE,          // Người dùng nâng cấp gói
    DOWNGRADE,        // Người dùng chủ động hạ gói
    AUTO_DOWNGRADE,   // Hệ thống tự động hạ gói khi hết hạn
    TRIAL_EXPIRED     // Gói thử nghiệm kết thúc
}
