// File: src/main/java/com/example/sales/controller/storefront/StorefrontController.java
package com.example.sales.controller.storefront;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.storefront.StorefrontOrderRequest;
import com.example.sales.dto.storefront.StorefrontOrderResponse;
import com.example.sales.dto.storefront.StorefrontProductResponse;
import com.example.sales.dto.storefront.StorefrontShopResponse;
import com.example.sales.service.storefront.StorefrontService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint công khai cho storefront (guest, không yêu cầu JWT).
 * Mọi path dưới {@code /api/storefront/**} đã được whitelist trong SecurityConfig.
 */
@RestController
@RequestMapping("/api/storefront")
@RequiredArgsConstructor
@Validated
@Tag(name = "Storefront", description = "Public storefront APIs for guest shopping")
public class StorefrontController {

    private final StorefrontService storefrontService;

    @GetMapping("/shops/{slug}")
    @Operation(summary = "Lấy thông tin shop công khai theo slug")
    public ApiResponseDto<StorefrontShopResponse> getShop(@PathVariable("slug") String slug) {
        return ApiResponseDto.success(ApiCode.SUCCESS, storefrontService.getShopBySlug(slug));
    }

    @GetMapping("/shops/{slug}/categories")
    @Operation(summary = "Danh sách category của shop")
    public ApiResponseDto<List<String>> getCategories(@PathVariable("slug") String slug) {
        return ApiResponseDto.success(ApiCode.SUCCESS, storefrontService.listCategories(slug));
    }

    @GetMapping("/shops/{slug}/products")
    @Operation(summary = "Danh sách sản phẩm công khai của shop")
    public ApiResponseDto<Page<StorefrontProductResponse>> getProducts(
            @PathVariable("slug") String slug,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            Pageable pageable) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                storefrontService.listProducts(slug, q, category, pageable));
    }

    @GetMapping("/shops/{slug}/products/{productId}")
    @Operation(summary = "Chi tiết sản phẩm")
    public ApiResponseDto<StorefrontProductResponse> getProduct(
            @PathVariable("slug") String slug,
            @PathVariable("productId") String productId) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                storefrontService.getProductDetail(slug, productId));
    }

    @PostMapping("/shops/{slug}/orders")
    @Operation(summary = "Đặt đơn guest (COD)")
    public ApiResponseDto<StorefrontOrderResponse> createOrder(
            @PathVariable("slug") String slug,
            @Valid @RequestBody StorefrontOrderRequest request) {
        return ApiResponseDto.success(ApiCode.STOREFRONT_ORDER_CREATED,
                storefrontService.createOrder(slug, request));
    }
}
