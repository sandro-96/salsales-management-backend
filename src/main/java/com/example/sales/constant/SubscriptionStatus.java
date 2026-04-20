// File: src/main/java/com/example/sales/constant/SubscriptionStatus.java
package com.example.sales.constant;

/**
 * Trạng thái của gói dịch vụ shop.
 *
 * <ul>
 *   <li>{@link #TRIAL}: dùng thử 30 ngày kể từ khi tạo shop, miễn phí, đầy đủ chức năng.</li>
 *   <li>{@link #ACTIVE}: đang thanh toán định kỳ (99k/tháng), chu kỳ hiện tại chưa hết hạn.</li>
 *   <li>{@link #EXPIRED}: hết hạn (trial hoặc chu kỳ trả phí). Shop bị khóa write
 *       thông qua {@code SubscriptionGuardInterceptor}, chỉ có thể đọc + thanh toán.</li>
 *   <li>{@link #CANCELLED}: shop/admin chủ động huỷ, không auto-charge nữa. Tương tự EXPIRED về mặt quyền.</li>
 * </ul>
 */
public enum SubscriptionStatus {
    TRIAL,
    ACTIVE,
    EXPIRED,
    CANCELLED
}
