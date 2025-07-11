// File: src/main/java/com/example/sales/service/ShopService.java
package com.example.sales.service;

import com.example.sales.constant.ApiErrorCode;
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
            throw new BusinessException(ApiErrorCode.SHOP_ALREADY_EXISTS);
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

    public Shop getMyShop(User user) {
        return shopRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new BusinessException(ApiErrorCode.SHOP_NOT_FOUND));
    }

    public Shop updateMyShop(User user, ShopRequest request) {
        Shop shop = getMyShop(user);

        shop.setName(request.getName());
        shop.setType(request.getType());
        shop.setAddress(request.getAddress());
        shop.setPhone(request.getPhone());
        shop.setLogoUrl(request.getLogoUrl());

        return shopRepository.save(shop);
    }
}
