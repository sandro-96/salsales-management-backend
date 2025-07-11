package com.example.sales.repository;

import com.example.sales.constant.OrderStatus;
import com.example.sales.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByUserId(String userId);
    List<Order> findAllByStatus(OrderStatus status);
    List<Order> findByShopId(String shopId);
    List<Order> findByShopIdAndBranchIdAndStatus(String shopId, String branchId, OrderStatus status);
}
