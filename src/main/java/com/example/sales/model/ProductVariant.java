package com.example.sales.model;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {
    private String variantId;
    private String name; // Ví dụ: "Size S", "Color Red"
    private String sku; // SKU riêng cho biến thể
    private double price; // Giá bán mặc định của biến thể
    private double costPrice; // Giá nhập của biến thể
    private Map<String, Object> attributes; // Thuộc tính bổ sung
}
