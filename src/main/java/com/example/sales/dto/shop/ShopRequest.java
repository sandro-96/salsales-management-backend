// File: src/main/java/com/example/sales/dto/shop/ShopRequest.java
package com.example.sales.dto.shop;

import com.example.sales.constant.ShopType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopRequest {
    private String name;
    private ShopType type;
    private String address;
    private String phone;
    private String logoUrl;
}
