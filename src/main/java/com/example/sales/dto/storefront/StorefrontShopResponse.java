// File: src/main/java/com/example/sales/dto/storefront/StorefrontShopResponse.java
package com.example.sales.dto.storefront;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Thông tin shop hiển thị trên trang bán hàng công khai (storefront).
 * Chỉ expose những trường an toàn cho guest user.
 */
@Data
@Builder
public class StorefrontShopResponse {
    private String id;
    private String name;
    private String slug;
    private String logoUrl;
    private String address;
    private String phone;
    private List<String> phones;
    private String currency;

    /** Liên kết mạng xã hội — hiển thị ở footer storefront. */
    private String zaloPageUrl;
    private String facebookUrl;
    private String tiktokUrl;
    private String shopeeUrl;

    /** Danh sách chi nhánh active — hiển thị ở footer cho khách biết các điểm bán. */
    private List<StorefrontBranchResponse> branches;
}
