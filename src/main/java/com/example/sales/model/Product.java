// File: src/main/java/com/example/sales/model/Product.java
package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("products")
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
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

    private String unit; // ví dụ: "kg", "cái", "hộp" (tuỳ loại cửa hàng)

    private String imageUrl;

    private String description;

    private String shopId;
    private String branchId;

    @Builder.Default
    private boolean active = true; // Có đang bán hay tạm ngưng
    private String productCode; // mã sản phẩm duy nhất
}
