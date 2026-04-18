// File: src/main/java/com/example/sales/repository/OrderRepository.java
package com.example.sales.repository;

import com.example.sales.constant.OrderStatus;
import com.example.sales.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends MongoRepository<Order, String> {
    Optional<Order> findByIdAndDeletedFalse(String id);

    Optional<Order> findByShopIdAndOrderCodeIgnoreCaseAndDeletedFalse(String shopId, String orderCode);

    Page<Order> findByShopIdAndBranchIdAndStatusAndDeletedFalse(
            String shopId, String branchId, OrderStatus status, Pageable pageable);

    Page<Order> findByShopIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(
            String shopId, OrderStatus status, Pageable pageable);

    Page<Order> findByShopIdAndDeletedFalseOrderByCreatedAtDesc(String shopId, Pageable pageable);

    Page<Order> findByShopIdAndBranchIdAndDeletedFalseOrderByCreatedAtDesc(
            String shopId, String branchId, Pageable pageable);

    @Query(value = "{ 'shopId': ?0, 'branchId': ?1, 'deleted': false, 'isPaid': false, 'status': { $nin: ?2 } }")
    Page<Order> findOpenOrdersByShopIdAndBranchId(
            String shopId, String branchId, List<OrderStatus> excludedStatuses, Pageable pageable);

    @Query(value = "{ 'shopId': ?0, 'deleted': false, 'isPaid': false, 'status': { $nin: ?1 }, '_id': { $in: ?2 } }")
    List<Order> findOpenOrdersByShopIdAndIdIn(
            String shopId, List<OrderStatus> excludedStatuses, List<String> ids);
}
