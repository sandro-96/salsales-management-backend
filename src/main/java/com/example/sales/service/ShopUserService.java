// File: src/main/java/com/example/sales/service/ShopUserService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.shop.ShopSimpleResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Shop;
import com.example.sales.model.ShopUser;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.ShopUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShopUserService extends BaseService {

    private final ShopUserRepository shopUserRepository;
    private final ShopRepository shopRepository;
    private final AuditLogService auditLogService;

    @Cacheable(value = "shopUsers", key = "#shopId + '-' + #userId")
    public ShopRole getUserRoleInShop(String shopId, String userId) {
        return checkShopUserExists(shopUserRepository, shopId, userId).getRole();
    }

    public boolean isOwner(String shopId, String userId) {
        return shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, userId)
                .map(shopUser -> shopUser.getRole() == ShopRole.OWNER)
                .orElse(false);
    }

    public boolean isStaff(String shopId, String userId) {
        return shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, userId)
                .map(shopUser -> shopUser.getRole() == ShopRole.STAFF)
                .orElse(false);
    }

    public boolean isOwnerOrStaff(String shopId, String userId) {
        return shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, userId)
                .map(shopUser -> {
                    ShopRole role = shopUser.getRole();
                    return role == ShopRole.OWNER || role == ShopRole.STAFF;
                })
                .orElse(false);
    }

    public void requireAnyRole(String shopId, String userId, ShopRole... roles) {
        ShopRole actual = getUserRoleInShop(shopId, userId);
        if (Arrays.stream(roles).noneMatch(role -> role == actual)) {
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }
    }

    public void requireOwner(String shopId, String userId) {
        if (!isOwner(shopId, userId)) {
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }
    }

    public void addUser(String shopId, String userId, ShopRole role, String branchId, String performedByUserId) {
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        if (!shop.isActive()) {
            throw new BusinessException(ApiCode.SHOP_INACTIVE);
        }

        Optional<ShopUser> existingShopUser = shopUserRepository.findByShopIdAndUserIdAndBranchId(shopId, userId, branchId);

        if (existingShopUser.isPresent()) {
            ShopUser shopUser = existingShopUser.get();
            if (!shopUser.isDeleted()) {
                throw new BusinessException(ApiCode.USER_ALREADY_IN_SHOP);
            } else {
                shopUser.setDeleted(false);
                shopUser.setRole(role);
                shopUserRepository.save(shopUser);
                auditLogService.log(performedByUserId, shopId, shopUser.getId(), "SHOP_USER", "REACTIVATED",
                        String.format("Tái kích hoạt người dùng %s vào chi nhánh %s của cửa hàng %s với vai trò %s", userId, branchId, shopId, role));
            }
        } else {
            ShopUser newShopUser = ShopUser.builder()
                    .shopId(shopId)
                    .userId(userId)
                    .role(role)
                    .branchId(branchId)
                    .build();
            shopUserRepository.save(newShopUser);
            auditLogService.log(performedByUserId, shopId, newShopUser.getId(), "SHOP_USER", "ADDED",
                    String.format("Thêm người dùng %s vào chi nhánh %s của cửa hàng %s với vai trò %s", userId, branchId, shopId, role));
        }
    }

    public void removeUser(String shopId, String userId, String branchId, String performedByUserId) {
        ShopUser shopUser = shopUserRepository.findByUserIdAndShopIdAndBranchIdAndDeletedFalse(userId, shopId, branchId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND));
        shopUser.setDeleted(true);
        shopUserRepository.save(shopUser);

        auditLogService.log(performedByUserId, shopId, shopUser.getId(), "SHOP_USER", "REMOVED",
                String.format("Xoá người dùng %s khỏi chi nhánh %s của cửa hàng %s", userId, branchId, shopId));
    }

    // ✅ Phương thức mới để xóa user khỏi shop, không quan tâm branch
    public void removeUserFromShop(String shopId, String userId, String performedByUserId) {
        // Tìm tất cả các bản ghi ShopUser của người dùng này trong shop này
        List<ShopUser> shopUsers = shopUserRepository.findByUserIdAndShopIdAndDeletedFalse(userId, shopId);

        if (shopUsers.isEmpty()) {
            throw new BusinessException(ApiCode.NOT_FOUND);
        }

        // Thực hiện xóa mềm cho tất cả các bản ghi tìm được
        for (ShopUser shopUser : shopUsers) {
            shopUser.setDeleted(true);
        }
        shopUserRepository.saveAll(shopUsers); // Lưu tất cả các bản ghi đã cập nhật

        auditLogService.log(performedByUserId, shopId, userId, "SHOP_USER", "REMOVED_FROM_SHOP",
                String.format("Xoá người dùng %s khỏi tất cả các chi nhánh của cửa hàng %s", userId, shopId));
    }

    public Page<ShopSimpleResponse> getShopsForUser(String userId, Pageable pageable) {
        return shopUserRepository.findByUserIdAndDeletedFalse(userId, pageable)
                .map(su -> shopRepository.findByIdAndDeletedFalse(su.getShopId())
                        .map(shop -> ShopSimpleResponse.builder()
                                .id(shop.getId())
                                .name(shop.getName())
                                .type(shop.getType())
                                .logoUrl(shop.getLogoUrl())
                                .active(shop.isActive())
                                .role(su.getRole())
                                .build())
                        .orElse(null));
    }
}
