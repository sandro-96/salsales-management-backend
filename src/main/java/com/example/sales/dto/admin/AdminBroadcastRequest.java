// File: src/main/java/com/example/sales/dto/admin/AdminBroadcastRequest.java
package com.example.sales.dto.admin;

import com.example.sales.constant.BroadcastAudience;
import com.example.sales.constant.SubscriptionPlan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class AdminBroadcastRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String message;

    @NotNull
    private BroadcastAudience audience;

    /** Bắt buộc khi audience = SHOPS_BY_PLAN. */
    private SubscriptionPlan plan;

    private boolean emailEnabled;
}
