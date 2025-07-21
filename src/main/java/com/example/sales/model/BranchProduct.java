// File: src/main/java/com/example/sales/model/BranchProduct.java
package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("branch_products")
@CompoundIndex(def = "{'productId':1, 'branchId':1}", unique = true) // Đảm bảo duy nhất mỗi sản phẩm trong mỗi chi nhánh
public class BranchProduct extends BaseEntity {
    @Id
    private String id;

    @NotBlank
    private String productId; // ID của sản phẩm trong Product collection

    @NotBlank
    private String shopId; // ID của Shop (denormalized for query efficiency)

    @NotBlank
    private String branchId; // ID của chi nhánh

    @Min(value = 0, message = "Số lượng không được âm")
    private int quantity; // Số lượng tồn kho tại chi nhánh này

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private double price; // Giá bán tại chi nhánh này

    @Builder.Default
    private boolean activeInBranch = true; // Trạng thái kích hoạt tại chi nhánh này
}