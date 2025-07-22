// File: src/main/java/com/example/sales/cache/ShopUserCache.java
package com.example.sales.cache;

import com.example.sales.constant.ShopRole;
import com.example.sales.repository.ShopUserRepository;
import com.example.sales.service.BaseService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class ShopUserCache extends BaseService {

    private final ShopUserRepository shopUserRepository;

    public ShopUserCache(ShopUserRepository shopUserRepository) {
        this.shopUserRepository = shopUserRepository;
    }

    @Cacheable(value = "shopUsers", key = "#shopId + '-' + #userId")
    public ShopRole getUserRoleInShop(String shopId, String userId) {
        return checkShopUserExists(shopUserRepository, shopId, userId).getRole();
    }
}
