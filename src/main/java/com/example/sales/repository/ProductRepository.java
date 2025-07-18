// File: src/main/java/com/example/sales/repository/ProductRepository.java
package com.example.sales.repository;

import com.example.sales.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findByIdAndDeletedFalse(String id);
    Optional<Product> findByIdAndShopIdAndDeletedFalse(String id, String shopId);
    Page<Product> findByShopIdAndDeletedFalse(String shopId, Pageable pageable);
    List<Product> findByShopIdAndQuantityLessThanAndDeletedFalse(String shopId, int threshold);
    List<Product> findByShopIdAndBranchIdAndDeletedFalse(String shopId, String branchId);
    List<Product> findByShopIdAndDeletedFalse(String shopId);
    @Query("{ 'shopId': ?0, 'deleted': false, $or: [ { 'name': { $regex: ?1, $options: 'i' } }, { 'sku': { $regex: ?1, $options: 'i' } }, { 'description': { $regex: ?1, $options: 'i' } } ] }")
    Page<Product> findByShopIdAndKeyword(String shopId, String keyword, Pageable pageable);
}
