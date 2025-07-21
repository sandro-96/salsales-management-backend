// File: src/main/java/com/example/sales/dto/inventory/InventoryRequest.java
package com.example.sales.dto.inventory;

import com.example.sales.constant.InventoryType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRequest {
    @NotBlank(message = "Branch ID không được để trống")
    private String branchId; // ID của chi nhánh

    @NotBlank(message = "Branch Product ID không được để trống")
    private String branchProductId; // ID của BranchProduct

    @NotNull(message = "Loại giao dịch không được để trống")
    private InventoryType type; // IMPORT, EXPORT, ADJUSTMENT

    @Min(value = 1, message = "Số lượng phải lớn hơn 0 cho IMPORT/EXPORT")
    // Đối với ADJUSTMENT, có thể chấp nhận newQuantity = 0, nhưng ở đây ta dùng quantity cho cả 3 loại
    private int quantity; // Số lượng thay đổi (cho IMPORT/EXPORT) hoặc số lượng mới (cho ADJUSTMENT)

    private String note;
    private String referenceId; // Dùng cho EXPORT, ví dụ: Order ID
}