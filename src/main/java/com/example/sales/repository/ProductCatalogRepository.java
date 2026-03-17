package com.example.sales.repository;

import com.example.sales.model.ProductCatalog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductCatalogRepository extends MongoRepository<ProductCatalog, String> {

    Optional<ProductCatalog> findByBarcode(String barcode);
}

