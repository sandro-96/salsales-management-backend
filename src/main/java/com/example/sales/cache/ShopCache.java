// File: src/main/java/com/example/sales/cache/ShopCache.java
package com.example.sales.cache;

import com.example.sales.constant.ApiCode;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Shop;
import com.example.sales.repository.ShopRepository;
import com.example.sales.service.BaseService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class ShopCache extends BaseService {

    private final ShopRepository shopRepository;

    public ShopCache(ShopRepository shopRepository) {
        this.shopRepository = shopRepository;
    }

    @Cacheable(value = "shops", key = "#ownerId")
    public Shop getShopByOwner(String ownerId) {
        return shopRepository.findByOwnerIdAndDeletedFalse(ownerId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
    }
}
