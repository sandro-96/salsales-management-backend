// File: src/main/java/com/example/sales/dto/admin/AdminShopPlanUpdateRequest.java
package com.example.sales.dto.admin;

import com.example.sales.constant.SubscriptionPlan;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminShopPlanUpdateRequest {

    @NotNull
    private SubscriptionPlan plan;

    /** Số tháng hiệu lực tính từ hiện tại. Bắt buộc > 0 với plan trả phí. */
    @Min(0)
    private int durationMonths;

    /** Ghi chú lý do (audit). */
    private String reason;
}
