// File: src/main/java/com/example/sales/dto/product/ProductRequest.java
package com.example.sales.dto.product;

import com.example.sales.model.ProductVariant;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO dùng để tạo hoặc cập nhật thông tin chung của sản phẩm (Product).
 * Chỉ chứa các trường thuộc về định nghĩa sản phẩm ở cấp shop.
 *
 * Các trường riêng của từng chi nhánh (price, quantity, branchCostPrice, discount...)
 * được quản lý riêng qua BranchProductRequest và API:
 *   PUT /shops/{shopId}/branches/{branchId}/products/{branchProductId}
 *
 * Lưu ý: priceHistory KHÔNG được truyền từ client — tự động ghi bởi server khi giá thay đổi.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private Map<String, String> nameTranslations; // Hỗ trợ đa ngôn ngữ

    private String category;

    @NotBlank(message = "SKU không được để trống")
    private String sku; // Duy nhất trong phạm vi shop

    @DecimalMin(value = "0.0", message = "Giá nhập mặc định không được âm")
    private double costPrice; // Giá nhập mặc định (dùng làm giá khởi tạo BranchProduct)

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá bán mặc định phải lớn hơn 0")
    private double defaultPrice; // Giá bán mặc định (dùng làm giá khởi tạo BranchProduct)

    @NotBlank(message = "Đơn vị tính không được để trống")
    private String unit;

    private String description;
    private List<String> images;

    private String barcode;
    private String supplierId;

    private List<ProductVariant> variants;

    private String reason; // Lý do thay đổi giá (tùy chọn, dùng khi update)

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean trackInventory = false;
}

