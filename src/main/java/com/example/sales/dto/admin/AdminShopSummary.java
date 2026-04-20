// File: src/main/java/com/example/sales/dto/admin/AdminShopSummary.java
package com.example.sales.dto.admin;

import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.constant.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tóm tắt shop cho list admin. Field {@code plan}/{@code planExpiry} chỉ còn cho backward-compat
 * khi rollback — UI mới dùng {@code subscriptionStatus}, {@code trialEndsAt}, {@code currentPeriodEnd}.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminShopSummary {
    private String id;
    private String name;
    private String slug;
    private String ownerId;
    private String ownerEmail;
    private String ownerName;

    /** @deprecated đọc từ {@code Shop.plan}, UI mới dựa vào {@link #subscriptionStatus}. */
    @Deprecated
    private SubscriptionPlan plan;
    /** @deprecated dùng {@link #currentPeriodEnd} hoặc {@link #trialEndsAt}. */
    @Deprecated
    private LocalDateTime planExpiry;

    // Subscription (nguồn sự thật mới) ─────────────────────────────
    private SubscriptionStatus subscriptionStatus;
    private LocalDateTime trialEndsAt;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime nextBillingDate;
    private long amountVnd;
    /** Số ngày còn lại của period/trial (0 nếu EXPIRED/CANCELLED). */
    private long daysRemaining;

    private boolean active;
    private LocalDateTime createdAt;
}
