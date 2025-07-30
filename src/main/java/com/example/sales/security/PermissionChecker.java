// File: src/main/java/com/example/sales/security/PermissionChecker.java
package com.example.sales.security;

import com.example.sales.constant.Permission;
import com.example.sales.constant.ShopRole;
import com.example.sales.repository.BranchRepository;
import com.example.sales.repository.ShopUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PermissionChecker {

    private final ShopUserRepository shopUserRepository;
    private final BranchRepository branchRepository;

    public boolean hasRole(String shopId, String userId, ShopRole... roles) {
        Set<ShopRole> allowed = Set.of(roles);
        return shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, userId)
                .map(user -> allowed.contains(user.getRole()))
                .orElse(false);
    }
    public boolean hasBranchRole(String branchId, String userId, ShopRole... roles) {
        return branchRepository.findByIdAndDeletedFalse(branchId)
                .map(branch -> {
                    // Ưu tiên kiểm tra ở branch trước
                    boolean branchMatch = shopUserRepository
                            .findByUserIdAndShopIdAndBranchIdAndDeletedFalse(branch.getShopId(), userId, branchId)
                            .map(su -> Arrays.asList(roles).contains(su.getRole()))
                            .orElse(false);
                    if (branchMatch) return true;

                    // Nếu không có ở branch, kiểm tra role cấp shop (nếu OWNER có toàn quyền)
                    return shopUserRepository
                            .findByShopIdAndUserIdAndDeletedFalse(branch.getShopId(), userId)
                            .stream()
                            .anyMatch(su -> su.getRole() == ShopRole.OWNER && Arrays.asList(roles).contains(su.getRole()));
                })
                .orElse(false);
    }


    public boolean hasPermission(String shopId, String branchId, String userId, Permission permission) {
        if (branchId != null && !branchId.isEmpty()) {
            // Kiểm tra quyền ở cấp chi nhánh trước
            return shopUserRepository.findByUserIdAndShopIdAndBranchIdAndDeletedFalse(shopId, userId, branchId)
                    .map(shopUser -> shopUser.getPermissions() != null && shopUser.getPermissions().contains(permission))
                    .orElse(false);
        } else {
            // Nếu không có branchId, kiểm tra quyền ở cấp shop
            return shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, userId)
                    .map(shopUser -> shopUser.getPermissions() != null && shopUser.getPermissions().contains(permission))
                    .orElse(false);
        }
    }
}
