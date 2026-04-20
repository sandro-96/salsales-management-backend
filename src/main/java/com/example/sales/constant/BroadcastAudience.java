// File: src/main/java/com/example/sales/constant/BroadcastAudience.java
package com.example.sales.constant;

public enum BroadcastAudience {
    /** Tất cả user chưa bị khoá. */
    ALL_USERS,
    /** Tất cả owner của shop (chủ cửa hàng). */
    SHOP_OWNERS,
    /** Lọc theo plan cụ thể (dùng field {@code plan}). */
    SHOPS_BY_PLAN,
    /** Admin hệ thống (ROLE_ADMIN). */
    ADMINS
}
