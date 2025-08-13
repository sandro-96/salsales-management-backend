// File: src/main/java/com/example/sales/dto/shop/ShopAdminResponse.java
package com.example.sales.dto.shop;

import com.example.sales.constant.BusinessModel;
import com.example.sales.constant.Country;
import com.example.sales.constant.ShopType;
import com.example.sales.constant.SubscriptionPlan;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ShopAdminResponse {
    private String id;
    private String name;
    private ShopType type;
    private BusinessModel businessModel;
    private Country countryCode;
    private String address;
    private String phone;
    private String logoUrl;
    private boolean active;
    private SubscriptionPlan plan;
    private String currency;
    private String timezone;
    private String orderPrefix;
    private LocalDateTime planExpiry;
}
