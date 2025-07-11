package com.example.sales.dto.promotion;

import com.example.sales.constant.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PromotionResponse {
    private String id;
    private String name;
    private DiscountType discountType;
    private double discountValue;
    private List<String> applicableProductIds;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean active;
}
