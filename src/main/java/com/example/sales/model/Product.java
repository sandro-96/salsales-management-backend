// File: src/main/java/com/example/sales/model/Product.java
package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
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
@Document("products")
// Đảm bảo SKU là duy nhất trong phạm vi mỗi shop
@CompoundIndex(def = "{'shopId': 1, 'sku': 1}", unique = true)
public class Product extends BaseEntity {
    @Id
    private String id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @NotBlank(message = "Danh mục không được để trống")
    private String category;

    @NotBlank(message = "SKU không được để trống")
    private String sku; // Mã sản phẩm SKU (duy nhất trong shop)

    private String shopId; // Shop mà định nghĩa sản phẩm này thuộc về
}