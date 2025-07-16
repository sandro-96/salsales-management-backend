// File: src/main/java/com/example/sales/controller/ShopUserController.java
package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponse;
import com.example.sales.model.User;
import com.example.sales.security.RequireRole;
import com.example.sales.service.ShopUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop-users")
@RequiredArgsConstructor
@Validated
public class ShopUserController {

    private final ShopUserService shopUserService;

    @GetMapping("/my")
    public ApiResponse<?> getMyShops(@AuthenticationPrincipal User user) {
        return ApiResponse.success(ApiCode.SUCCESS, shopUserService.getShopsForUser(user.getId()));
    }

    @PostMapping("/add")
    @RequireRole(ShopRole.OWNER)
    public ApiResponse<?> addUser(@RequestParam String shopId,
                                  @RequestParam String userId,
                                  @RequestParam ShopRole role) {
        shopUserService.addUser(shopId, userId, role);
        return ApiResponse.success(ApiCode.SUCCESS);
    }

    @DeleteMapping("/remove")
    @RequireRole(ShopRole.OWNER)
    public ApiResponse<?> removeUser(@RequestParam String shopId,
                                     @RequestParam String userId) {
        shopUserService.removeUser(shopId, userId);
        return ApiResponse.success(ApiCode.SUCCESS);
    }
}
