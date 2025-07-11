package com.example.sales.dto.promotion;

import com.example.sales.constant.DiscountType;
import jakarta.validation.constraints.*;
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

    private List<String> applicableProductIds;

    @NotNull
    private LocalDateTime startDate;

    @NotNull
    private LocalDateTime endDate;

    private boolean active = true;
    private String branchId;
}
