// File: src/main/java/com/example/sales/security/PermissionChecker.java
package com.example.sales.security;

import com.example.sales.constant.ShopRole;
import com.example.sales.repository.ShopUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionChecker {

    private final ShopUserRepository shopUserRepository;

    public boolean isOwner(String shopId, String userId) {
        return shopUserRepository.findByShopIdAndUserId(shopId, userId)
                .map(shopUser -> shopUser.getRole() == ShopRole.OWNER)
                .orElse(false);
    }

    public boolean isStaff(String shopId, String userId) {
        return shopUserRepository.findByShopIdAndUserId(shopId, userId)
                .map(shopUser -> shopUser.getRole() == ShopRole.STAFF)
                .orElse(false);
    }

    public boolean isOwnerOrStaff(String shopId, String userId) {
        return shopUserRepository.findByShopIdAndUserId(shopId, userId)
                .map(shopUser -> {
                    ShopRole role = shopUser.getRole();
                    return role == ShopRole.OWNER || role == ShopRole.STAFF;
                }).orElse(false);
    }
}
