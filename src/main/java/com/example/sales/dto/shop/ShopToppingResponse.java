package com.example.sales.dto.shop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopToppingResponse {
    private String toppingId;
    private String name;
    private double extraPrice;
    private boolean active;
}
