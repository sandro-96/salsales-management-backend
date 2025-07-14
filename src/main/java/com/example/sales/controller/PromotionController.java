// File: src/main/java/com/example/sales/controller/PromotionController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.promotion.PromotionRequest;
import com.example.sales.dto.promotion.PromotionResponse;
import com.example.sales.model.User;
import com.example.sales.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ApiResponse<List<PromotionResponse>> getAll(@AuthenticationPrincipal User user,
                                                       @RequestParam(required = false) String branchId) {
        return ApiResponse.success(ApiCode.SUCCESS, promotionService.getAll(user, branchId));
    }

    @PostMapping
    public ApiResponse<PromotionResponse> create(@AuthenticationPrincipal User user,
                                                 @RequestBody @Valid PromotionRequest request) {
        return ApiResponse.success(ApiCode.SUCCESS, promotionService.create(user, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PromotionResponse> update(@AuthenticationPrincipal User user,
                                                 @PathVariable String id,
                                                 @RequestBody @Valid PromotionRequest request) {
        return ApiResponse.success(ApiCode.SUCCESS, promotionService.update(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@AuthenticationPrincipal User user,
                                 @PathVariable String id) {
        promotionService.delete(user, id);
        return ApiResponse.success(ApiCode.SUCCESS);
    }
}
