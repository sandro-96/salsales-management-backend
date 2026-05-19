// File: src/main/java/com/example/sales/dto/storefront/StorefrontBranchResponse.java
package com.example.sales.dto.storefront;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

/**
 * Thông tin chi nhánh hiển thị trên footer storefront công khai.
 * Chỉ expose dữ liệu an toàn cho khách (không expose taxRegistrationNumber,
 * managerName, managerPhone, wifiPassword,…).
 */
@Data
@Builder
public class StorefrontBranchResponse {
    private String id;
    private String name;
    private String address;
    private String phone;
    private List<String> phones;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private boolean isDefault;
}
