// File: src/main/java/com/example/sales/repository/ProductRepository.java
package com.example.sales.repository;

import com.example.sales.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho sản phẩm.
 */
public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findByIdAndDeletedFalse(String id);
    Optional<Product> findByIdAndShopIdAndDeletedFalse(String id, String shopId);
    Page<Product> findByShopIdAndDeletedFalse(String shopId, Pageable pageable);
    List<Product> findByShopIdAndQuantityLessThanAndDeletedFalse(String shopId, int threshold);

}
