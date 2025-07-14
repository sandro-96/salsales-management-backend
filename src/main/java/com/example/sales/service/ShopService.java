// File: src/main/java/com/example/sales/service/ShopService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ShopRequest;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;

    public Shop createShop(User owner, ShopRequest request) {
        if (shopRepository.findByOwnerId(owner.getId()).isPresent()) {
            throw new BusinessException(ApiCode.SHOP_ALREADY_EXISTS);
        }

        Shop shop = new Shop();
        shop.setName(request.getName());
        shop.setType(request.getType());
        shop.setAddress(request.getAddress());
        shop.setPhone(request.getPhone());
        shop.setLogoUrl(request.getLogoUrl());
        shop.setOwnerId(owner.getId());

        return shopRepository.save(shop);
    }

    public Shop getShopByOwner(String ownerId) {
        return shopRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
    }

    public Shop updateShop(String ownerId, ShopRequest request) {
        Shop shop = getShopByOwner(ownerId);

        shop.setName(request.getName());
        shop.setType(request.getType());
        shop.setAddress(request.getAddress());
        shop.setPhone(request.getPhone());
        shop.setLogoUrl(request.getLogoUrl());

        return shopRepository.save(shop);
    }

    public String getShopIdByOwner(String ownerId) {
        return shopRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND))
                .getId();
    }
}
