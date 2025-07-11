// File: src/main/java/com/example/sales/repository/OrderRepository.java
package com.example.sales.repository;

import com.example.sales.constant.OrderStatus;
import com.example.sales.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Order> findByUserId(String userId);

    @Query("{ 'status': ?0, 'deleted': false }")
    List<Order> findAllByStatus(OrderStatus status);

    @Query("{ 'shopId': ?0, 'deleted': false }")
    List<Order> findByShopId(String shopId);

    @Query("{ 'shopId': ?0, 'branchId': ?1, 'status': ?2, 'deleted': false }")
    List<Order> findByShopIdAndBranchIdAndStatus(String shopId, String branchId, OrderStatus status);
}
