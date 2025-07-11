package com.example.sales.controller;

import com.example.sales.constant.ApiMessage;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.promotion.PromotionRequest;
import com.example.sales.dto.promotion.PromotionResponse;
import com.example.sales.model.User;
import com.example.sales.service.PromotionService;
import com.example.sales.util.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;
    private final MessageService messageService;

    @GetMapping
    public ApiResponse<List<PromotionResponse>> getAll(@AuthenticationPrincipal User user,
                                                       @RequestParam(required = false) String branchId,
                                                       Locale locale) {
        return ApiResponse.success(ApiMessage.SUCCESS, promotionService.getAll(user, branchId), messageService, locale);
    }

    @PostMapping
    public ApiResponse<PromotionResponse> create(@AuthenticationPrincipal User user,
                                                 @RequestBody @Valid PromotionRequest request,
                                                 Locale locale) {
        return ApiResponse.success(ApiMessage.SUCCESS, promotionService.create(user, request), messageService, locale);
    }

    @PutMapping("/{id}")
    public ApiResponse<PromotionResponse> update(@AuthenticationPrincipal User user,
                                                 @PathVariable String id,
                                                 @RequestBody @Valid PromotionRequest request,
                                                 Locale locale) {
        return ApiResponse.success(ApiMessage.SUCCESS, promotionService.update(user, id, request), messageService, locale);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@AuthenticationPrincipal User user,
                                 @PathVariable String id,
                                 Locale locale) {
        promotionService.delete(user, id);
        return ApiResponse.success(ApiMessage.SUCCESS, messageService, locale);
    }
}
