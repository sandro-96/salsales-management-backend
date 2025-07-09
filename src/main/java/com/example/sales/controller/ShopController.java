package com.example.sales.controller;

import com.example.sales.constant.ApiMessage;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.ShopRequest;
import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.service.ShopService;
import com.example.sales.util.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final MessageService messageService;

    @PostMapping
    public ApiResponse<Shop> createShop(@AuthenticationPrincipal User user,
                                        @RequestBody ShopRequest request, Locale locale) {
        Shop created = shopService.createShop(user, request);
        return ApiResponse.success(ApiMessage.SUCCESS, created, messageService, locale);
    }

    @GetMapping("/me")
    public ApiResponse<Shop> getMyShop(@AuthenticationPrincipal User user, Locale locale) {
        Shop shop = shopService.getMyShop(user);
        return ApiResponse.success(ApiMessage.SUCCESS, shop, messageService, locale);
    }

    @PutMapping
    public ApiResponse<Shop> updateMyShop(@AuthenticationPrincipal User user,
                                          @RequestBody ShopRequest request, Locale locale) {
        Shop updated = shopService.updateMyShop(user, request);
        return ApiResponse.success(ApiMessage.SUCCESS, updated, messageService, locale);
    }
}
