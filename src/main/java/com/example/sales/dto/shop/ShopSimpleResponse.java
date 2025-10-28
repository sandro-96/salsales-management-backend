// File: src/main/java/com/example/sales/dto/shop/ShopSimpleResponse.java
package com.example.sales.dto.shop;

import com.example.sales.constant.*;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShopSimpleResponse {
    private String id;
    private String name;
    private ShopType type;
    private String logoUrl;
    private Country countryCode;
    private String phone;
    private String address;
    private String slug;
    private boolean active;
    private boolean isTrackInventory;
    private BusinessModel businessModel;
    private ShopRole role;
    private ShopIndustry industry;
}
