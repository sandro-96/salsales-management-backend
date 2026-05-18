// File: src/main/java/com/example/sales/dto/storefront/TableContextResponse.java
package com.example.sales.dto.storefront;

import lombok.Builder;
import lombok.Data;

/**
 * Response cho {@code GET /api/storefront/tables/{qrToken}} — đủ context để FE render
 * trang menu cho khách: thông tin shop, bàn, branch (để hiển thị địa chỉ).
 */
@Data
@Builder
public class TableContextResponse {

    private StorefrontShopResponse shop;
    private TableInfo table;

    @Data
    @Builder
    public static class TableInfo {
        private String id;
        private String name;
        private String branchId;
        private String branchName;
        private String branchAddress;
        private Integer capacity;
    }
}
