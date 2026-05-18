package com.example.sales.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserOrderHistoryLineItem {
    private String productName;
    private String variantName;
    private String sku;
    private int quantity;
    private double unitPrice;
    private double lineTotal;
}
