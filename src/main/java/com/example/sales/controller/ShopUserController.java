package com.example.sales.controller;

import com.example.sales.constant.ApiMessage;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponse;
import com.example.sales.model.User;
import com.example.sales.service.ShopUserService;
import com.example.sales.util.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/shop-users")
@RequiredArgsConstructor
public class ShopUserController {

    private final ShopUserService shopUserService;
    private final MessageService messageService;

    @PostMapping("/add")
    public ApiResponse<?> addUser(@AuthenticationPrincipal User admin,
                                  @RequestParam String shopId,
                                  @RequestParam String userId,
                                  @RequestParam ShopRole role,
                                  Locale locale) {
        if (shopUserService.isOwner(shopId, admin.getId())) {
            throw new com.example.sales.exception.BusinessException(
                    com.example.sales.constant.ApiErrorCode.UNAUTHORIZED);
        }
        shopUserService.addUser(shopId, userId, role);
        return ApiResponse.success(ApiMessage.SUCCESS, messageService, locale);
    }

    @DeleteMapping("/remove")
    public ApiResponse<?> removeUser(@AuthenticationPrincipal User admin,
                                     @RequestParam String shopId,
                                     @RequestParam String userId,
                                     Locale locale) {
        if (shopUserService.isOwner(shopId, admin.getId())) {
            throw new com.example.sales.exception.BusinessException(
                    com.example.sales.constant.ApiErrorCode.UNAUTHORIZED);
        }
        shopUserService.removeUser(shopId, userId);
        return ApiResponse.success(ApiMessage.SUCCESS, messageService, locale);
    }
}
