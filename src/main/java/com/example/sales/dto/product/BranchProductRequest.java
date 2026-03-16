package com.example.sales.dto.product;

import com.example.sales.model.BranchProductVariant;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO dùng để cập nhật thông tin BranchProduct (dữ liệu riêng tại từng chi nhánh).
 * Tách biệt hoàn toàn khỏi ProductRequest (thông tin chung của shop).
 * Được dùng cho API: PUT /shops/{shopId}/branches/{branchId}/products/{branchProductId}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchProductRequest {

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá bán phải lớn hơn 0")
    private double price; // Giá bán tại chi nhánh

    @DecimalMin(value = "0.0", message = "Giá nhập tại chi nhánh không được âm")
    private double branchCostPrice; // Giá nhập tại chi nhánh

    @Min(value = 0, message = "Số lượng không được âm")
    private int quantity; // Số lượng tồn kho tại chi nhánh

    @Min(value = 0, message = "Số lượng tối thiểu không được âm")
    private int minQuantity; // Ngưỡng cảnh báo tồn kho thấp

    private Double discountPrice; // Giá khuyến mãi (null = không có khuyến mãi)
    private Double discountPercentage; // Phần trăm giảm giá (null = không có)

    private LocalDate expiryDate; // Hạn sử dụng (dành cho thực phẩm, thuốc...)

    private List<BranchProductVariant> branchVariants; // Biến thể tại chi nhánh

    private String reason; // Lý do thay đổi giá (tùy chọn, dùng khi ghi priceHistory)
}

