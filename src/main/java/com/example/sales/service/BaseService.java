// File: src/main/java/com/example/sales/service/BaseService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Order;
import com.example.sales.model.Shop;
import com.example.sales.model.ShopUser;
import com.example.sales.repository.OrderRepository;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.ShopUserRepository;

public abstract class BaseService {
    protected Shop checkShopExists(ShopRepository shopRepository, String shopId) {
        return shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
    }
    protected ShopUser checkShopUserExists(ShopUserRepository shopUserRepository, String shopId, String userId) {
        return shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, userId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND));
    }
    protected Order checkOrderExists(OrderRepository orderRepository, String orderId, String shopId) {
        return orderRepository.findByIdAndDeletedFalse(orderId)
                .filter(o -> o.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.ORDER_NOT_FOUND));
    }
}
