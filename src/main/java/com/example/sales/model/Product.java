package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("products")
@CompoundIndex(def = "{'shopId': 1, 'sku': 1}", unique = true)
public class Product extends BaseEntity {
    @Id
    private String id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private Map<String, String> nameTranslations; // Hỗ trợ đa ngôn ngữ

    private String categoryId;

    @NotBlank(message = "SKU không được để trống")
    private String sku;

    @NotBlank(message = "Shop ID không được để trống")
    private String shopId;

    @DecimalMin(value = "0.0", message = "Giá nhập không được âm")
    private double costPrice; // Giá nhập mặc định

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá bán mặc định phải lớn hơn 0")
    private double defaultPrice; // Giá bán mặc định

    @NotBlank(message = "Đơn vị không được để trống")
    private String unit; // Đơn vị đo lường (kg, lít, cái, v.v.)

    private String description; // Mô tả sản phẩm
    private List<String> images; // Danh sách URL hình ảnh

    @Builder.Default
    private boolean active = true; // Trạng thái sản phẩm

    private String barcode; // Mã vạch (cho bán lẻ)
    private String supplierId; // ID nhà cung cấp

    private List<ProductVariant> variants; // Biến thể sản phẩm
    private List<PriceHistory> priceHistory; // Lịch sử giá

    @DBRef
    private Category category;
}