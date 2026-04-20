// File: src/main/java/com/example/sales/dto/admin/AdminShopPlanExtendRequest.java
package com.example.sales.dto.admin;

import jakarta.validation.constraints.Min;
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
public class AdminShopPlanExtendRequest {

    @Min(1)
    private int months;

    private String reason;
}
