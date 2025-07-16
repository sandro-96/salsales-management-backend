// File: src/main/java/com/example/sales/dto/shopUser/ShopUserResponse.java
package com.example.sales.dto.shopUser;

import com.example.sales.constant.ShopRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShopUserResponse {
    private String shopId;
    private String shopName;
    private ShopRole role;
}
