package com.example.sales.dto.shop;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopToppingRequest {

    /** Giữ nguyên khi cập nhật; để trống khi tạo mới — server gán UUID */
    private String toppingId;

    @NotBlank(message = "Tên topping không được để trống")
    private String name;

    @DecimalMin(value = "0.0", message = "Giá phụ topping không âm")
    private double extraPrice;

    @Builder.Default
    private boolean active = true;
}
