// File: src/main/java/com/example/sales/dto/inventory/InventoryWeightRequest.java
package com.example.sales.dto.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload cho API nhập/xuất tồn theo cân (base unit gram/ml).
 * Dùng cho sản phẩm {@code sellByWeight = true}; server sẽ quy đổi
 * {@code weight} về base unit qua {@link com.example.sales.util.WeightUnitConverter}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryWeightRequest {

    @NotBlank(message = "Branch ID không được để trống")
    private String branchId;

    @NotBlank(message = "Branch Product ID không được để trống")
    private String branchProductId;

    /**
     * Trọng lượng/volume nhập vào (ví dụ 0.5 khi unit=kg = 500g).
     * Server chuyển về base unit để lưu trong {@code stockInBaseUnits}.
     */
    @NotNull(message = "Trọng lượng không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Trọng lượng phải > 0")
    private Double weight;

    /**
     * Đơn vị weight. Null = dùng {@code Product.unit} (mặc định).
     * Chấp nhận: kg, g, l, ml.
     */
    private String unit;

    private String note;

    /** Dùng cho EXPORT: reference đơn hàng (tuỳ chọn). */
    private String referenceId;
}
