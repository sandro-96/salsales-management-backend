package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("branch_products")
public class BranchProduct extends BaseEntity {
    @Id
    private String id;

    @NotBlank(message = "Product ID không được để trống")
    private String productId;

    @NotBlank(message = "Shop ID không được để trống")
    private String shopId;

    @NotBlank(message = "Branch ID không được để trống")
    private String branchId;

    @Min(value = 0, message = "Số lượng không được âm")
    private int quantity; // Số lượng tồn kho tại chi nhánh

    @Min(value = 0, message = "Số lượng tối thiểu không được âm")
    private int minQuantity; // Số lượng tối thiểu để cảnh báo nhập hàng

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá bán phải lớn hơn 0")
    private double price; // Giá bán tại chi nhánh

    @DecimalMin(value = "0.0", message = "Giá nhập không được âm")
    private double branchCostPrice; // Giá nhập tại chi nhánh

    private Double discountPrice; // Giá khuyến mãi
    private Double discountPercentage; // Phần trăm giảm giá

    private LocalDate expiryDate; // Hạn sử dụng (cho thực phẩm, thuốc)

    @Builder.Default
    private boolean activeInBranch = true; // Trạng thái tại chi nhánh

    private List<BranchProductVariant> variants; // Biến thể tại chi nhánh

    // (Tùy chọn) Tham chiếu trực tiếp đến Product, Shop, Branch
    @DBRef
    private Product product;
    @DBRef
    private Shop shop;
    @DBRef
    private Branch branch;
}