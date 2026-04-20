// File: src/main/java/com/example/sales/dto/admin/AdminBillingOverviewResponse.java
package com.example.sales.dto.admin;

import com.example.sales.constant.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Tổng quan billing cho admin sau khi đơn giản hoá:
 *
 * <ul>
 *   <li>{@code mrrVnd} = COUNT(ACTIVE) × 99.000.</li>
 *   <li>{@code subscriptionStatusDistribution}: đếm shop theo status (TRIAL/ACTIVE/EXPIRED/CANCELLED).</li>
 *   <li>{@code activePaidShops}, {@code trialShops}, {@code expiringIn7Days}, {@code expiredUnrenewed}:
 *       các counter phục vụ dunning.</li>
 *   <li>Trend giữ nguyên format (MonthlyPoint) để UI không phải đổi chart.</li>
 * </ul>
 */
@Getter
@Builder
public class AdminBillingOverviewResponse {

    private long mrrVnd;
    private long activePaidShops;
    private long trialShops;
    private long expiringIn7Days;
    private long expiredUnrenewed;

    private Map<SubscriptionStatus, Long> subscriptionStatusDistribution;

    private List<MonthlyPoint> mrrTrend;
    private List<MonthlyPoint> newSubscriptions;
    private List<MonthlyPoint> renewals;

    @Getter
    @Builder
    public static class MonthlyPoint {
        private String month; // YYYY-MM
        private long value;
    }
}
