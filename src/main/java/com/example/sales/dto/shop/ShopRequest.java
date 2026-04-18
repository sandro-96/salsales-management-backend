// File: src/main/java/com/example/sales/dto/shop/ShopRequest.java
package com.example.sales.dto.shop;

import com.example.sales.constant.BusinessModel;
import com.example.sales.constant.Country;
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

    private Country countryCode;

    /** Mã số thuế (MST), tùy chọn */
    private String taxRegistrationNumber;

    private String zaloPageUrl;
    private String facebookUrl;
    private String tiktokUrl;
    private String shopeeUrl;

    /** Bật topping cho cửa hàng (mặc định false khi không gửi) */
    private Boolean toppingsEnabled;

    private boolean active = true;
}
