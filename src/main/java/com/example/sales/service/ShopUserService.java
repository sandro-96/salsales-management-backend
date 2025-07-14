// File: src/main/java/com/example/sales/service/ShopUserService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.ShopUser;
import com.example.sales.repository.ShopUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShopUserService {

    private final ShopUserRepository shopUserRepository;

    public ShopRole getUserRoleInShop(String shopId, String userId) {
        return shopUserRepository.findByShopIdAndUserId(shopId, userId)
                .map(ShopUser::getRole)
                .orElseThrow(() -> new BusinessException(ApiCode.UNAUTHORIZED));
    }

    public boolean isOwner(String shopId, String userId) {
        return getUserRoleInShop(shopId, userId) != ShopRole.OWNER;
    }

    public void requireAnyRole(String shopId, String userId, ShopRole... roles) {
        ShopRole actual = getUserRoleInShop(shopId, userId);
        if (Arrays.stream(roles).noneMatch(role -> role == actual)) {
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }
    }

    public void requireOwner(String shopId, String userId) {
        if (isOwner(shopId, userId)) {
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }
    }

    public void addUser(String shopId, String userId, ShopRole role) {
        Optional<ShopUser> existing = shopUserRepository.findByShopIdAndUserId(shopId, userId);
        if (existing.isPresent()) {
            throw new BusinessException(ApiCode.DUPLICATE_DATA);
        }

        ShopUser shopUser = ShopUser.builder()
                .shopId(shopId)
                .userId(userId)
                .role(role)
                .build();

        shopUserRepository.save(shopUser);
    }

    public void removeUser(String shopId, String userId) {
        ShopUser user = shopUserRepository.findByShopIdAndUserId(shopId, userId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND));
        shopUserRepository.delete(user);
    }
}
