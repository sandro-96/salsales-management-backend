// File: src/main/java/com/example/sales/controller/ShopController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.ShopRequest;
import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
@Validated
public class ShopController {

    private final ShopService shopService;

    @PostMapping
    public ApiResponse<Shop> createShop(@AuthenticationPrincipal User user,
                                        @RequestBody ShopRequest request) {
        Shop created = shopService.createShop(user, request);
        return ApiResponse.success(ApiCode.SUCCESS, created);
    }

    @GetMapping("/me")
    public ApiResponse<?> getMyShop(@AuthenticationPrincipal User user) {
        Shop shop = shopService.getMyShop(user);
        if (shop == null) {
            return ApiResponse.error(ApiCode.SHOP_NOT_FOUND);
        }
        return ApiResponse.success(ApiCode.SUCCESS, shop);
    }

    @PutMapping
    public ApiResponse<Shop> updateMyShop(@AuthenticationPrincipal User user,
                                          @RequestBody ShopRequest request) {
        Shop updated = shopService.updateMyShop(user, request);
        return ApiResponse.success(ApiCode.SUCCESS, updated);
    }
}
