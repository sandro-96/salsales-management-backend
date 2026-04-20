// File: src/main/java/com/example/sales/dto/admin/AdminShopDetail.java
package com.example.sales.dto.admin;

import com.example.sales.model.SubscriptionHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Chi tiết shop cho admin — extend {@link AdminShopSummary} bằng thống kê và
 * lịch sử plan.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminShopDetail {
    private AdminShopSummary summary;
    private long branchCount;
    private long staffCount;
    private long totalOrderCount;
    private long orderCountLast30d;
    private List<SubscriptionHistory> subscriptionHistory;
}
