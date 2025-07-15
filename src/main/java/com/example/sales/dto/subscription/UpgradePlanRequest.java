// File: com/example/sales/dto/subscription/UpgradePlanRequest.java
package com.example.sales.dto.subscription;

import com.example.sales.constant.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpgradePlanRequest {
    @NotNull
    private SubscriptionPlan targetPlan;
    private int months = 1; // số tháng đăng ký (default = 1)
}
