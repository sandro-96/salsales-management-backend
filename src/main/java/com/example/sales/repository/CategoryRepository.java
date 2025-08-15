package com.example.sales.repository;

import com.example.sales.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {
    Optional<Category> findByIdAndShopIdAndDeletedFalse(String id, String shopId);
}
