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

    @Min(value = 0, message = "Số lượng không được âm")
    // IMPORT/EXPORT: quantity > 0 được validate thêm tại service layer
    // ADJUSTMENT: quantity = 0 hợp lệ (ví dụ: zero-out tồn kho khi kiểm kê)
    private int quantity;

    private String note;
    private String referenceId; // Dùng cho EXPORT, ví dụ: Order ID
}