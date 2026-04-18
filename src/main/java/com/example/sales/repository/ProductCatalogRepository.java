package com.example.sales.repository;

import com.example.sales.model.ProductCatalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.regex.Pattern;

@Repository
public interface ProductCatalogRepository extends MongoRepository<ProductCatalog, String> {

    Optional<ProductCatalog> findByBarcode(String barcode);

    Page<ProductCatalog> findByNameRegex(Pattern pattern, Pageable pageable);
}

