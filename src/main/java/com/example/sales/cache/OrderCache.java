// File: src/main/java/com/example/sales/cache/OrderCache.java
package com.example.sales.cache;

import com.example.sales.constant.ApiCode;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Order;
import com.example.sales.repository.OrderRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class OrderCache {

    private final OrderRepository orderRepository;

    public OrderCache(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Cacheable(value = "orders", key = "#shopId + ':' + #orderId")
    public Order getOrderByShop(String orderId, String shopId) {
        return orderRepository.findByIdAndDeletedFalse(orderId)
                .filter(o -> o.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.ORDER_NOT_FOUND));
    }
}
