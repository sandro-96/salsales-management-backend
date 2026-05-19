// File: src/main/java/com/example/sales/dto/shop/ShopResponse.java
package com.example.sales.dto.shop;

import com.example.sales.constant.BusinessModel;
import com.example.sales.constant.Country;
import com.example.sales.constant.ShopType;
import com.example.sales.constant.SubscriptionPlan;
import lombok.Builder;
import lombok.Data;

import java.util.List;

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
    private List<String> phones;
    private String taxRegistrationNumber;
    private String zaloPageUrl;
    private String facebookUrl;
    private String tiktokUrl;
    private String shopeeUrl;
    private boolean toppingsEnabled;
    private boolean onlineSalesEnabled;
    private boolean tableOrderingEnabled;
    private String logoUrl;
    private boolean active;
    private SubscriptionPlan plan;
    private String currency;
    private String slug;
}
