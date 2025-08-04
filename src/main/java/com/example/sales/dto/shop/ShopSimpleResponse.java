// File: src/main/java/com/example/sales/dto/shop/ShopSimpleResponse.java
package com.example.sales.dto.shop;

import com.example.sales.constant.ShopIndustry;
import com.example.sales.constant.ShopRole;
import com.example.sales.constant.ShopType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShopSimpleResponse {
    private String id;
    private String name;
    private ShopType type;
    private String logoUrl;
    private String countryCode;
    private String phone;
    private String address;
    private boolean active;
    private boolean isTrackInventory;
    private ShopRole role;
    private ShopIndustry industry;
}
