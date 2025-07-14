// File: src/main/java/com/example/sales/controller/ShopController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.ShopRequest;
import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
@Validated
public class ShopController {

    private final ShopService shopService;

    @PostMapping
    public ApiResponse<Shop> create(@AuthenticationPrincipal User user,
                                    @RequestBody @Valid ShopRequest request) {
        return ApiResponse.success(ApiCode.SUCCESS, shopService.createShop(user, request));
    }

    @GetMapping("/me")
    public ApiResponse<Shop> getMyShop(@AuthenticationPrincipal User user) {
        return ApiResponse.success(ApiCode.SUCCESS, shopService.getShopByOwner(user.getId()));
    }

    @PutMapping("/me")
    public ApiResponse<Shop> update(@AuthenticationPrincipal User user,
                                    @RequestBody @Valid ShopRequest request) {
        return ApiResponse.success(ApiCode.SUCCESS, shopService.updateShop(user.getId(), request));
    }
}
