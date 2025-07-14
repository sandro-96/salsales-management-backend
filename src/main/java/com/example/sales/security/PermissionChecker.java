// File: src/main/java/com/example/sales/security/PermissionChecker.java
package com.example.sales.security;

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
    private final BranchRepository branchRepository;

    public boolean isOwner(String shopId, String userId) {
        return hasRole(shopId, userId, ShopRole.OWNER);
    }

    public boolean isStaff(String shopId, String userId) {
        return hasRole(shopId, userId, ShopRole.STAFF);
    }

    public boolean isOwnerOrStaff(String shopId, String userId) {
        return hasRole(shopId, userId, ShopRole.OWNER, ShopRole.STAFF);
    }

    public boolean isAdmin(String shopId, String userId) {
        return hasRole(shopId, userId, ShopRole.ADMIN);
    }

    public boolean hasRole(String shopId, String userId, ShopRole... roles) {
        Set<ShopRole> allowed = Set.of(roles);
        return shopUserRepository.findByShopIdAndUserId(shopId, userId)
                .map(user -> allowed.contains(user.getRole()))
                .orElse(false);
    }
    public boolean hasBranchRole(String branchId, String userId, ShopRole... roles) {
        return branchRepository.findById(branchId)
                .map(branch -> hasRole(branch.getShopId(), userId, roles))
                .orElse(false);
    }
}
