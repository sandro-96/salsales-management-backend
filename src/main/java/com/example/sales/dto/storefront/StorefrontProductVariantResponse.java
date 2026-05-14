// File: src/main/java/com/example/sales/dto/storefront/StorefrontProductVariantResponse.java
package com.example.sales.dto.storefront;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StorefrontProductVariantResponse {
    private String variantId;
    private String name;
    private double price;
    private List<String> images;
}
