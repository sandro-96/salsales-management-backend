// File: src/main/java/com/example/sales/controller/ShopController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.shop.ShopRequest;
import com.example.sales.dto.shop.ShopSimpleResponse;
import com.example.sales.model.Shop;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.service.FileUploadService;
import com.example.sales.service.ShopService;
import com.example.sales.service.ShopUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
@Validated
public class ShopController {

    private final ShopService shopService;
    private final ShopUserService shopUserService;
    private final FileUploadService fileUploadService;

    @PostMapping(consumes = "multipart/form-data")
    public ApiResponse<Shop> create(@AuthenticationPrincipal CustomUserDetails user,
                                    @RequestPart("shop") @Valid ShopRequest request,
                                    @RequestPart(value = "file", required = false) MultipartFile file) {
        String logoUrl = null;
        if (file != null && !file.isEmpty()) {
            logoUrl = fileUploadService.upload(file);
        }
        return ApiResponse.success(ApiCode.SUCCESS, shopService.createShop(user.getId(), request, logoUrl));
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
