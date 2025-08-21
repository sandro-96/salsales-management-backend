// File: src/main/java/com/example/sales/service/ProductService.java
package com.example.sales.service;

import com.example.sales.dto.product.ProductRequest;
import com.example.sales.dto.product.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(String shopId, List<String> branchIds, ProductRequest request);

    // id ở đây là id của BranchProduct
    ProductResponse updateProduct(String userId, String shopId, List<String> branchIds, String id, ProductRequest request);

    // id ở đây là id của BranchProduct
    void deleteProduct(String userId, String shopId, String branchId, String id);

    // id ở đây là id của BranchProduct
    ProductResponse getProduct(String shopId, String branchId, String id);

    // branchProductId ở đây là id của BranchProduct để toggle activeInBranch
    ProductResponse toggleActive(String userId, String shopId, String branchId, String branchProductId);

    List<ProductResponse> getLowStockProducts(String shopId, String branchId, int threshold);

    String getSuggestedSku(String shopId, String industry, String category);

    String getSuggestedBarcode(String shopId, String industry, String category);
}