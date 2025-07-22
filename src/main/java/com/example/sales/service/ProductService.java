// File: src/main/java/com/example/sales/service/ProductService.java
package com.example.sales.service;

import com.example.sales.dto.product.ProductRequest;
import com.example.sales.dto.product.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(String shopId, String branchId, ProductRequest request);

    // id ở đây là id của BranchProduct
    ProductResponse updateProduct(String userId, String shopId, String branchId, String id, ProductRequest request);

    // id ở đây là id của BranchProduct
    void deleteProduct(String userId, String shopId, String branchId, String id);

    // id ở đây là id của BranchProduct
    ProductResponse getProduct(String shopId, String branchId, String id);

    // productId ở đây là id của BranchProduct để toggle activeInBranch
    ProductResponse toggleActive(String userId, String shopId, String branchId, String branchProductId);

    List<ProductResponse> getLowStockProducts(String shopId, String branchId, int threshold);

    Page<ProductResponse> searchProducts(String shopId, String branchId, String keyword, Pageable pageable);
}