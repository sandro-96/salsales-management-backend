// File: src/main/java/com/example/sales/dto/admin/AdminBroadcastResponse.java
package com.example.sales.dto.admin;

import com.example.sales.constant.BroadcastAudience;
import com.example.sales.constant.BroadcastStatus;
import com.example.sales.constant.SubscriptionPlan;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminBroadcastResponse {
    private String id;
    private String title;
    private String message;
    private BroadcastAudience audience;
    private SubscriptionPlan plan;
    private boolean emailEnabled;
    private BroadcastStatus status;
    private Integer recipientCount;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private String createdBy;
}
