// File: src/main/java/com/example/sales/controller/ShopController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.shop.ShopRequest;
import com.example.sales.dto.shop.ShopSimpleResponse;
import com.example.sales.model.Shop;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.service.ShopService;
import com.example.sales.service.ShopUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
@Validated
public class ShopController {

    private final ShopService shopService;
    private final ShopUserService shopUserService;

    @PostMapping
    public ApiResponse<Shop> create(@AuthenticationPrincipal CustomUserDetails user,
                                    @RequestBody @Valid ShopRequest request) {
        return ApiResponse.success(ApiCode.SUCCESS, shopService.createShop(user.getId(), request));
    }

    @GetMapping("/me")
    public ApiResponse<?> getMyShop(@AuthenticationPrincipal CustomUserDetails user) {
        Shop shop = shopService.getShopByOwner(user.getId());
        return ApiResponse.success(ApiCode.SUCCESS, shopService.getShopResponse(user, shop));
    }

    @PutMapping("/me")
    public ApiResponse<Shop> update(@AuthenticationPrincipal CustomUserDetails user,
                                    @RequestBody @Valid ShopRequest request) {
        return ApiResponse.success(ApiCode.SUCCESS, shopService.updateShop(user.getId(), request));
    }
    @GetMapping("/my")
    public ApiResponse<List<ShopSimpleResponse>> getMyShops(@AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.success(ApiCode.SUCCESS, shopUserService.getShopsForUser(user.getId()));
    }

    @DeleteMapping
    public ApiResponse<?> deleteShop(@AuthenticationPrincipal CustomUserDetails user) {
        shopService.deleteShop(user.getId());
        return ApiResponse.success(ApiCode.SUCCESS);
    }
}
