// File: src/main/java/com/example/sales/service/ProductService.java
package com.example.sales.service;

import com.example.sales.dto.product.ProductRequest;
import com.example.sales.dto.product.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for managing product-related operations.
 */
public interface ProductService {

    /**
     * Creates a new product for a specific shop.
     *
     * @param shopId  the ID of the shop
     * @param request the product details
     * @return the created product's response
     */
    ProductResponse createProduct(String shopId, ProductRequest request);

    /**
     * Updates an existing product.
     *
     * @param userId  the ID of the user performing the update
     * @param shopId  the ID of the shop
     * @param id      the ID of the product to update
     * @param request the updated product details
     * @return the updated product's response
     */
    ProductResponse updateProduct(String userId, String shopId, String id, ProductRequest request);

    /**
     * Soft deletes a product from the specified shop.
     *
     * @param shopId the ID of the shop
     * @param id     the ID of the product to delete
     */
    void deleteProduct(String shopId, String id);

    /**
     * Retrieves details of a specific product.
     *
     * @param shopId the ID of the shop
     * @param id     the ID of the product
     * @return the product's response
     */
    ProductResponse getProduct(String shopId, String id);

    /**
     * Retrieves a paginated list of products for a shop.
     *
     * @param shopId   the ID of the shop
     * @param pageable the pagination information
     * @return a paginated list of product responses
     */
    Page<ProductResponse> getAllByShop(String shopId, Pageable pageable);

    /**
     * Toggles the active status of a product.
     *
     * @param shopId    the ID of the shop
     * @param productId the ID of the product
     * @return the updated product's response
     */
    ProductResponse toggleActive(String shopId, String productId);

    /**
     * Retrieves a list of products with low stock levels.
     *
     * @param shopId   the ID of the shop
     * @param threshold the low stock threshold
     * @return a list of products with low stock
     */
    List<ProductResponse> getLowStockProducts(String shopId, int threshold);
}
