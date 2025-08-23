package com.example.sales.cache;

import com.example.sales.dto.product.ProductResponse;
import com.example.sales.model.BranchProduct;
import com.example.sales.model.Product;
import org.springframework.stereotype.Component;

/**
 * Utility class to map Product and BranchProduct to ProductResponse.
 */
@Component
public class ProductMapper {

    public ProductResponse toResponse(BranchProduct branchProduct, Product product) {
        if (branchProduct == null && product == null) {
            return null;
        }
        ProductResponse.ProductResponseBuilder builder = ProductResponse.builder()
                .productId(product.getId())
                .name(product.getName())
                .nameTranslations(product.getNameTranslations())
                .category(product.getCategory())
                .sku(product.getSku())
                .costPrice(product.getCostPrice())
                .defaultPrice(product.getDefaultPrice())
                .unit(product.getUnit())
                .description(product.getDescription())
                .images(product.getImages())
                .barcode(product.getBarcode())
                .supplierId(product.getSupplierId())
                .variants(product.getVariants())
                .priceHistory(product.getPriceHistory())
                .active(product.isActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt());

        if (branchProduct != null) {
            builder.id(branchProduct.getId())
                    .quantity(branchProduct.getQuantity())
                    .minQuantity(branchProduct.getMinQuantity())
                    .price(branchProduct.getPrice())
                    .branchCostPrice(branchProduct.getBranchCostPrice())
                    .discountPrice(branchProduct.getDiscountPrice())
                    .discountPercentage(branchProduct.getDiscountPercentage())
                    .expiryDate(branchProduct.getExpiryDate())
                    .branchVariants(branchProduct.getVariants())
                    .branchId(branchProduct.getBranchId())
                    .activeInBranch(branchProduct.isActiveInBranch())
                    .createdAt(branchProduct.getCreatedAt())
                    .updatedAt(branchProduct.getUpdatedAt());
        }
        return builder.build();
    }
}