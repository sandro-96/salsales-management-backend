// File: src/main/java/com/example/sales/dto/product/ProductRequest.java
package com.example.sales.dto.product;

import com.example.sales.model.BranchProductVariant;
import com.example.sales.model.PriceHistory;
import com.example.sales.model.ProductVariant;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTO dùng để tạo hoặc cập nhật thông tin sản phẩm tại một chi nhánh cụ thể.
 * Các trường như name, categoryId, sku, costPrice, defaultPrice, unit, description, images, barcode, supplierId, variants, recipe, priceHistory sẽ ảnh hưởng đến định nghĩa sản phẩm chung (Product).
 * Các trường như price, quantity, branchCostPrice, discountPrice, discountPercentage, expiryDate, minQuantity, active sẽ ảnh hưởng đến BranchProduct.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    // Các trường ảnh hưởng đến Product (định nghĩa chung)
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private Map<String, String> nameTranslations; // Hỗ trợ đa ngôn ngữ

    private String categoryId; // Thay vì category string, sử dụng ID tham chiếu đến Category (tùy chọn)

    @NotBlank(message = "SKU không được để trống")
    private String sku; // SKU là duy nhất trong phạm vi shop, dùng để tìm/tạo Product chính

    @DecimalMin(value = "0.0", message = "Giá nhập không được âm")
    private double costPrice; // Giá nhập mặc định

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá bán mặc định phải lớn hơn 0")
    private double defaultPrice; // Giá bán mặc định

    @NotBlank(message = "Đơn vị tính không được để trống")
    private String unit;

    private String description;
    private List<String> images; // Danh sách URL hình ảnh

    private String barcode; // Mã vạch (cho bán lẻ)
    private String supplierId; // ID nhà cung cấp

    private List<ProductVariant> variants; // Biến thể sản phẩm
    private List<PriceHistory> priceHistory; // Lịch sử giá

    // Các trường ảnh hưởng đến BranchProduct (chi tiết tại chi nhánh)
    @Min(value = 0, message = "Số lượng không được âm")
    private int quantity;

    @Min(value = 0, message = "Số lượng tối thiểu không được âm")
    private int minQuantity;

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá bán phải lớn hơn 0")
    private double price;

    @DecimalMin(value = "0.0", message = "Giá nhập tại chi nhánh không được âm")
    private double branchCostPrice;

    private Double discountPrice; // Giá khuyến mãi
    private Double discountPercentage; // Phần trăm giảm giá

    private LocalDate expiryDate; // Hạn sử dụng

    private List<BranchProductVariant> branchVariants; // Biến thể tại chi nhánh

    // active sẽ được ánh xạ tới activeInBranch của BranchProduct
    @Builder.Default
    private boolean active = true;
}