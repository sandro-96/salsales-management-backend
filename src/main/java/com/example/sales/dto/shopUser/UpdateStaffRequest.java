package com.example.sales.dto.shopUser;

import com.example.sales.constant.Permission;
import com.example.sales.constant.ShopRole;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateStaffRequest {
    private ShopRole role;
    private Set<Permission> permissions;
}
