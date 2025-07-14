// File: src/main/java/com/example/sales/controller/PromotionController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.promotion.PromotionRequest;
import com.example.sales.dto.promotion.PromotionResponse;
import com.example.sales.security.RequireRole;
import com.example.sales.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@Validated
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<List<PromotionResponse>> getAll(@RequestParam String shopId,
                                                       @RequestParam(required = false) String branchId) {
        return ApiResponse.success(ApiCode.SUCCESS, promotionService.getAll(shopId, branchId));
    }

    @PostMapping
    @RequireRole(ShopRole.OWNER)
    public ApiResponse<PromotionResponse> create(@RequestParam String shopId,
                                                 @RequestBody @Valid PromotionRequest request) {
        return ApiResponse.success(ApiCode.SUCCESS, promotionService.create(shopId, request));
    }

    @PutMapping("/{id}")
    @RequireRole(ShopRole.OWNER)
    public ApiResponse<PromotionResponse> update(@RequestParam String shopId,
                                                 @PathVariable String id,
                                                 @RequestBody @Valid PromotionRequest request) {
        return ApiResponse.success(ApiCode.SUCCESS, promotionService.update(shopId, id, request));
    }

    @DeleteMapping("/{id}")
    @RequireRole(ShopRole.OWNER)
    public ApiResponse<?> delete(@RequestParam String shopId,
                                 @PathVariable String id) {
        promotionService.delete(shopId, id);
        return ApiResponse.success(ApiCode.SUCCESS);
    }
}
