// File: src/main/java/com/example/sales/service/ShopContextResolver.java
package com.example.sales.service;

import com.example.sales.cache.ShopCache;
import com.example.sales.constant.ApiCode;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Shop;
import com.example.sales.model.ShopUser;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.ShopUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Xác định shop theo ngữ cảnh request — subscription/billing và guard phải theo đúng shop,
 * không dùng {@code findFirst} theo owner khi user có nhiều shop.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopContextResolver {

    private final ShopRepository shopRepository;
    private final ShopCache shopCache;
    private final ShopUserRepository shopUserRepository;

    /**
     * Gợi ý shopId: query {@code shopId} trước, sau đó header {@code X-Shop-Id}.
     */
    public static String shopIdHintFrom(HttpServletRequest request) {
        String q = request.getParameter("shopId");
        if (StringUtils.hasText(q)) {
            return q.trim();
        }
        String h = request.getHeader("X-Shop-Id");
        return StringUtils.hasText(h) ? h.trim() : null;
    }

    /**
     * Subscription/billing: chủ shop hoặc user được mời ({@code shop_users}) của shop đó.
     */
    public Shop resolveShopForSubscription(String userId, String shopIdHint) {
        if (!StringUtils.hasText(shopIdHint)) {
            Set<String> shopIds = collectDistinctShopIdsForUser(userId);
            if (shopIds.isEmpty()) {
                throw new BusinessException(ApiCode.SHOP_NOT_FOUND);
            }
            if (shopIds.size() > 1) {
                log.warn("[ShopContext] user {} gắn {} shop (chủ/mời), thiếu shopId hoặc header X-Shop-Id",
                        userId, shopIds.size());
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }
            Shop shop = shopCache.getShopById(shopIds.iterator().next());
            assertUserHasSubscriptionAccess(userId, shop);
            return shop;
        }
        Shop shop = shopCache.getShopById(shopIdHint.trim());
        assertUserHasSubscriptionAccess(userId, shop);
        return shop;
    }

    private void assertUserHasSubscriptionAccess(String userId, Shop shop) {
        if (userId.equals(shop.getOwnerId())) {
            return;
        }
        if (shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shop.getId(), userId).isPresent()) {
            return;
        }
        throw new BusinessException(ApiCode.ACCESS_DENIED);
    }

    /** Shop do user làm chủ hoặc được gán qua shop_users (chưa xoá). */
    private Set<String> collectDistinctShopIdsForUser(String userId) {
        Set<String> shopIds = new LinkedHashSet<>();
        for (Shop s : shopRepository.findAllByOwnerIdAndDeletedFalse(userId)) {
            shopIds.add(s.getId());
        }
        for (ShopUser su : shopUserRepository.findAllByUserIdAndDeletedFalse(userId)) {
            if (StringUtils.hasText(su.getShopId())) {
                shopIds.add(su.getShopId());
            }
        }
        return shopIds;
    }

    /**
     * Guard subscription khi ghi: owner hoặc nhân sự được gán vào shop (query/header có shopId).
     */
    public Shop resolveShopForWriteGuard(String userId, String shopIdHint) {
        if (!StringUtils.hasText(shopIdHint)) {
            Set<String> shopIds = collectDistinctShopIdsForUser(userId);
            if (shopIds.isEmpty()) {
                return null;
            }
            if (shopIds.size() > 1) {
                log.warn("[ShopContext] user {} có {} shop nhưng thiếu shopId/X-Shop-Id trên request ghi",
                        userId, shopIds.size());
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }
            return shopCache.getShopById(shopIds.iterator().next());
        }
        Shop shop = shopCache.getShopById(shopIdHint.trim());
        if (userId.equals(shop.getOwnerId())) {
            return shop;
        }
        if (shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shop.getId(), userId).isPresent()) {
            return shop;
        }
        throw new BusinessException(ApiCode.ACCESS_DENIED);
    }
}
