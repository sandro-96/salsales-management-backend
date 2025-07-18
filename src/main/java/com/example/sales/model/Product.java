// File: src/main/java/com/example/sales/model/Product.java
package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Mô hình sản phẩm trong cơ sở dữ liệu.
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("products")
public class Product extends BaseEntity {
    @Id
    private String id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @NotBlank(message = "Danh mục không được để trống")
    private String category;

    @Min(value = 0, message = "Số lượng không được âm")
    private int quantity;

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private double price;

    private String unit;
    private String imageUrl;
    private String description;

    private String shopId;
    private String branchId;

    @Builder.Default
    private boolean active = true;

    private String productCode;
    private String sku;
}
