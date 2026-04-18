package com.example.sales.cache;

import com.example.sales.dto.product.ProductResponse;
import com.example.sales.dto.shop.ShopToppingResponse;
import com.example.sales.model.BranchProduct;
import com.example.sales.model.Product;
import com.example.sales.model.Shop;
import com.example.sales.model.ShopTopping;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class to map Product and BranchProduct to ProductResponse.
 */
@Component
public class ProductMapper {

    public ProductResponse toResponse(BranchProduct branchProduct, Product product, Shop shop) {
        if (branchProduct == null && product == null) {
            return null;
        }
        ProductResponse.ProductResponseBuilder builder = ProductResponse.builder()
                .productId(product.getId())
                .shopId(product.getShopId())
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
                .trackInventory(product.isTrackInventory())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt());

        enrichToppings(builder, product, shop);

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
                    .branchPriceHistory(branchProduct.getPriceHistory())
                    .createdAt(branchProduct.getCreatedAt())
                    .updatedAt(branchProduct.getUpdatedAt());
        }
        return builder.build();
    }

    private static void enrichToppings(ProductResponse.ProductResponseBuilder builder, Product product, Shop shop) {
        List<String> assigned = product.getAssignedToppingIds() != null
                ? new ArrayList<>(product.getAssignedToppingIds())
                : List.of();
        builder.assignedToppingIds(assigned.isEmpty() ? List.of() : assigned);

        if (shop == null) {
            builder.toppingsEnabled(false);
            builder.applicableToppings(List.of());
            return;
        }
        boolean enabled = shop.isToppingsEnabled();
        builder.toppingsEnabled(enabled);
        if (!enabled || shop.getShopToppings() == null || shop.getShopToppings().isEmpty()) {
            builder.applicableToppings(List.of());
            return;
        }

        Set<String> allowed = new LinkedHashSet<>();
        for (String id : assigned) {
            if (StringUtils.hasText(id)) {
                allowed.add(id.trim());
            }
        }
        if (allowed.isEmpty()) {
            builder.applicableToppings(List.of());
            return;
        }

        List<ShopToppingResponse> applicable = shop.getShopToppings().stream()
                .filter(ShopTopping::isActive)
                .filter(t -> t.getToppingId() != null && allowed.contains(t.getToppingId().trim()))
                .map(t -> ShopToppingResponse.builder()
                        .toppingId(t.getToppingId())
                        .name(t.getName())
                        .extraPrice(t.getExtraPrice())
                        .active(t.isActive())
                        .build())
                .sorted(Comparator.comparing(ShopToppingResponse::getToppingId, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());

        // Fallback: khớp toppingId không phân biệt hoa thường nếu danh sách gán lệch casing
        if (applicable.isEmpty()) {
            for (ShopTopping t : shop.getShopToppings()) {
                if (!t.isActive() || !StringUtils.hasText(t.getToppingId())) {
                    continue;
                }
                String tid = t.getToppingId().trim();
                boolean match = allowed.stream().anyMatch(a -> a.equalsIgnoreCase(tid));
                if (match) {
                    applicable.add(ShopToppingResponse.builder()
                            .toppingId(tid)
                            .name(t.getName())
                            .extraPrice(t.getExtraPrice())
                            .active(t.isActive())
                            .build());
                }
            }
            applicable.sort(Comparator.comparing(ShopToppingResponse::getToppingId, Comparator.nullsLast(String::compareToIgnoreCase)));
        }

        builder.applicableToppings(applicable);
    }
}
