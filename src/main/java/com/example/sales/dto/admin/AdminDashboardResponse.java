// File: src/main/java/com/example/sales/dto/admin/AdminDashboardResponse.java
package com.example.sales.dto.admin;

import com.example.sales.constant.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Payload dashboard admin: KPI tổng quan + phân bố plan + series tăng trưởng 30 ngày.
 */
@Getter
@Builder
public class AdminDashboardResponse {

    private long totalShops;
    private long activeShops;
    private long newShopsThisMonth;

    private long totalUsers;
    private long newUsersThisMonth;
    private long totalAdmins;

    /** Số lượng shop theo {@link SubscriptionStatus}. */
    private Map<SubscriptionStatus, Long> subscriptionStatusDistribution;

    private long openTickets;
    private long urgentTickets;

    /**
     * MRR ước lượng (VND) — đếm subscription {@code ACTIVE} × 99.000.
     */
    private long mrrVnd;

    /** Series 30 ngày gần nhất: new shops / day. */
    private List<DailyPoint> newShopsTrend;
    /** Series 30 ngày gần nhất: new users / day. */
    private List<DailyPoint> newUsersTrend;

    @Getter
    @Builder
    public static class DailyPoint {
        private String date; // yyyy-MM-dd
        private long count;
    }
}
