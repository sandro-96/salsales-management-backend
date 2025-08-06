// File: src/main/java/com/example/sales/dto/shop/ShopRequest.java
package com.example.sales.dto.shop;

import com.example.sales.constant.BusinessModel;
import com.example.sales.constant.ShopType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShopRequest {
    @NotBlank(message = "Tên cửa hàng không được để trống")
    private String name;

    @NotNull(message = "Loại cửa hàng là bắt buộc")
    private ShopType type;

    private BusinessModel businessModel;

    private String phone;

    private String address;

    private String countryCode;
}
