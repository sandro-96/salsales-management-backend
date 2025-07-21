// File: src/main/java/com/example/sales/model/BranchProduct.java
package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("branch_products") // <-- Collection mới cho BranchProduct
public class BranchProduct extends BaseEntity {

    @Id
    private String id;

    @NotBlank(message = "Product ID không được để trống")
    private String productId; // Liên kết với Product (định nghĩa chung)

    @NotBlank(message = "Shop ID không được để trống")
    private String shopId;

    @NotBlank(message = "Branch ID không được để trống")
    private String branchId; // Chi nhánh mà sản phẩm này thuộc về

    @Min(value = 0, message = "Số lượng không được âm")
    private int quantity;

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private double price;

    @NotBlank(message = "Đơn vị không được để trống")
    private String unit;

    private String imageUrl;
    private String description;

    @Builder.Default
    private boolean activeInBranch = true; // Trạng thái kích hoạt tại chi nhánh này
}