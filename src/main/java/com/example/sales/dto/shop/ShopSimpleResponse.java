// File: src/main/java/com/example/sales/dto/shop/ShopSimpleResponse.java
package com.example.sales.dto.shop;

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
    private boolean active;
    private ShopRole role; // 👈 vai trò của user trong shop
    private String branchId;     // ✅ Chi nhánh user thuộc về
    private String branchName;   // ✅ Tên chi nhánh
    private String branchAddress;// ✅ Địa chỉ chi nhánh
}
