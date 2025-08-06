// File: src/main/java/com/example/sales/security/PermissionChecker.java
package com.example.sales.security;

import com.example.sales.constant.Permission;
import com.example.sales.constant.ShopRole;
import com.example.sales.repository.BranchRepository;
import com.example.sales.repository.ShopUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class PermissionChecker {

    private final ShopUserRepository shopUserRepository;

    public boolean hasRole(String shopId, String userId, ShopRole... roles) {
        Set<ShopRole> allowed = Set.of(roles);
        return shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, userId)
                .map(user -> allowed.contains(user.getRole()))
                .orElse(false);
    }

    public boolean hasPermission(String shopId, String userId, Permission permission) {
        return shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, userId)
                .map(shopUser -> shopUser.getPermissions() != null && shopUser.getPermissions().contains(permission))
                .orElse(false);
    }
}
