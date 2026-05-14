// File: src/main/java/com/example/sales/dto/promotion/PromotionRequest.java
package com.example.sales.dto.promotion;

import com.example.sales.constant.DiscountType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromotionRequest {

    @NotBlank
    private String name;

    @NotNull
    private DiscountType discountType;

    @Positive
    private double discountValue;

    /**
     * Ưu tiên khi trùng điều kiện (càng lớn càng áp dụng trước). Mặc định 0.
     */
    @Min(0)
    @Max(1_000_000)
    private Integer priority;

    private List<String> applicableProductIds;

    @NotNull
    private LocalDateTime startDate;

    @NotNull
    private LocalDateTime endDate;

    private boolean active = true;
    private String branchId;
}
