// File: src/main/java/com/example/sales/dto/admin/AdminSubscriptionHistoryItem.java
package com.example.sales.dto.admin;

import com.example.sales.constant.SubscriptionActionType;
import com.example.sales.constant.SubscriptionPlan;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminSubscriptionHistoryItem {
    private String id;
    private String shopId;
    private String shopName;
    private String userId;

    private SubscriptionPlan oldPlan;
    private SubscriptionPlan newPlan;
    private int durationMonths;

    private String transactionId;
    private String paymentMethod;
    private SubscriptionActionType actionType;

    private LocalDateTime createdAt;
}
