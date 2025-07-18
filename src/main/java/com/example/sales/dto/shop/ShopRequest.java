// File: src/main/java/com/example/sales/dto/shop/ShopRequest.java
package com.example.sales.dto.shop;

import com.example.sales.constant.ShopType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopRequest {
    @NotBlank(message = "Tên cửa hàng không được để trống")
    private String name;

    @NotNull(message = "Loại cửa hàng là bắt buộc")
    private ShopType type;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    private String address;
}
