package com.example.sales.dto.shopUser;

import com.example.sales.constant.ShopRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShopStaffResponse {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String avatarUrl;
    private String shopId;
    private String userId;
    private String shopName;
    private String branchId;
    private String branchName;
    private String branchAddress;
    private ShopRole role;
}
