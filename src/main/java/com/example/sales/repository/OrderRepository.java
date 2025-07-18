// File: src/main/java/com/example/sales/repository/OrderRepository.java
package com.example.sales.repository;

import com.example.sales.constant.OrderStatus;
import com.example.sales.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends MongoRepository<Order, String> {
    Optional<Order> findByIdAndDeletedFalse(String id);
    List<Order> findByShopIdAndDeletedFalse(String shopId);
    List<Order> findByShopIdAndBranchIdAndStatusAndDeletedFalse(String shopId, String branchId, OrderStatus status);
    Page<Order> findByShopIdAndDeletedFalse(String shopId, Pageable pageable);
    Page<Order> findByShopIdAndBranchIdAndStatusAndDeletedFalse(String shopId, String branchId, OrderStatus status, Pageable pageable);
}
