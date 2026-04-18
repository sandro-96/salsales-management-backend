package com.example.sales.controller.shop;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.Permission;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.shop.ShopToppingRequest;
import com.example.sales.dto.shop.ShopToppingResponse;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequirePermission;
import com.example.sales.service.ShopToppingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop/{shopId}/toppings")
@RequiredArgsConstructor
@Validated
public class ShopToppingController {

    private final ShopToppingService shopToppingService;

    @GetMapping
    @RequirePermission(Permission.SHOP_VIEW)
    @Operation(summary = "Danh sách topping shop")
    public ApiResponseDto<List<ShopToppingResponse>> list(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable("shopId") String shopId) {
        return ApiResponseDto.success(ApiCode.SUCCESS, shopToppingService.listToppings(shopId));
    }

    @PutMapping
    @RequirePermission(Permission.SHOP_UPDATE)
    @Operation(summary = "Cập nhật toàn bộ danh mục topping shop")
    public ApiResponseDto<List<ShopToppingResponse>> replace(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable("shopId") String shopId,
            @Valid @RequestBody List<ShopToppingRequest> body) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                shopToppingService.replaceToppings(user.getId(), shopId, body));
    }
}
