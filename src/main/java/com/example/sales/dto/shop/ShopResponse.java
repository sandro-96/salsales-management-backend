// File: src/main/java/com/example/sales/dto/shop/ShopResponse.java
package com.example.sales.dto.shop;

import com.example.sales.constant.BusinessModel;
import com.example.sales.constant.Country;
import com.example.sales.constant.ShopType;
import com.example.sales.constant.SubscriptionPlan;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShopResponse {
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
    private String slug;
}
