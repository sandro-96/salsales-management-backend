// File: src/main/java/com/example/sales/dto/admin/AdminUserDetail.java
package com.example.sales.dto.admin;

import com.example.sales.constant.ShopRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserDetail {
    private AdminUserSummary summary;
    private List<ShopMembership> memberships;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ShopMembership {
        private String shopId;
        private String shopName;
        private ShopRole role;
        private boolean owner;
    }
}
