package com.example.sales.model;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchProductVariant {
    private String variantId; // Liên kết với variantId trong Product
    private int quantity; // Số lượng tồn kho của biến thể
    private double price; // Giá bán của biến thể tại chi nhánh
    private double branchCostPrice; // Giá nhập của biến thể
    private Double discountPrice; // Giá khuyến mãi
    private Double discountPercentage; // Phần trăm giảm giá
}
