// File: src/main/java/com/example/sales/controller/storefront/TableOrderingController.java
package com.example.sales.controller.storefront;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.storefront.StorefrontProductResponse;
import com.example.sales.dto.storefront.TableContextResponse;
import com.example.sales.dto.storefront.TableOrderRequest;
import com.example.sales.dto.storefront.TableOrderResponse;
import com.example.sales.service.storefront.StorefrontService;
import com.example.sales.service.storefront.TableOrderingService;
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
 * Endpoint công khai cho QR self-ordering tại bàn.
 *
 * <p>Mount tại {@code /api/storefront/tables/**} → đã nằm trong whitelist
 * {@code SecurityConfig}: không yêu cầu JWT. Khách quét QR → FE mở
 * {@code /t/{shopSlug}/{qrToken}} → gọi các endpoint này.
 */
@RestController
@RequestMapping("/api/storefront/tables")
@RequiredArgsConstructor
@Validated
@Tag(name = "Storefront — Table QR Ordering",
        description = "Guest self-service ordering tại bàn qua QR code")
public class TableOrderingController {

    private final TableOrderingService tableOrderingService;
    private final StorefrontService storefrontService;

    @GetMapping("/{qrToken}")
    @Operation(summary = "Lấy context bàn (shop + table + branch) theo qrToken")
    public ApiResponseDto<TableContextResponse> getContext(@PathVariable("qrToken") String qrToken) {
        return ApiResponseDto.success(ApiCode.SUCCESS, tableOrderingService.getContext(qrToken));
    }

    @GetMapping("/{qrToken}/categories")
    @Operation(summary = "Danh sách category của shop chứa bàn")
    public ApiResponseDto<List<String>> getCategories(@PathVariable("qrToken") String qrToken) {
        TableOrderingService.ResolvedTable rt = tableOrderingService.resolve(qrToken);
        return ApiResponseDto.success(ApiCode.SUCCESS,
                storefrontService.listCategories(rt.shop().getSlug()));
    }

    @GetMapping("/{qrToken}/products")
    @Operation(summary = "Danh sách sản phẩm public của shop chứa bàn")
    public ApiResponseDto<Page<StorefrontProductResponse>> getProducts(
            @PathVariable("qrToken") String qrToken,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            Pageable pageable) {
        TableOrderingService.ResolvedTable rt = tableOrderingService.resolve(qrToken);
        return ApiResponseDto.success(ApiCode.SUCCESS,
                storefrontService.listProducts(rt.shop().getSlug(), q, category, pageable));
    }

    @GetMapping("/{qrToken}/products/{productId}")
    @Operation(summary = "Chi tiết sản phẩm")
    public ApiResponseDto<StorefrontProductResponse> getProduct(
            @PathVariable("qrToken") String qrToken,
            @PathVariable("productId") String productId) {
        TableOrderingService.ResolvedTable rt = tableOrderingService.resolve(qrToken);
        return ApiResponseDto.success(ApiCode.SUCCESS,
                storefrontService.getProductDetail(rt.shop().getSlug(), productId));
    }

    @GetMapping("/{qrToken}/current-order")
    @Operation(summary = "Lấy đơn đang mở của bàn (nếu có)")
    public ApiResponseDto<TableOrderResponse> getCurrentOrder(@PathVariable("qrToken") String qrToken) {
        return ApiResponseDto.success(ApiCode.SUCCESS, tableOrderingService.getCurrentOrder(qrToken));
    }

    @PostMapping("/{qrToken}/orders")
    @Operation(summary = "Gửi đơn — append vào đơn đang mở hoặc tạo mới")
    public ApiResponseDto<TableOrderResponse> placeOrder(
            @PathVariable("qrToken") String qrToken,
            @Valid @RequestBody TableOrderRequest request) {
        TableOrderingService.CreatedOrAppended result = tableOrderingService.placeOrder(qrToken, request);
        ApiCode code = result.created()
                ? ApiCode.IN_STORE_ORDER_CREATED
                : ApiCode.IN_STORE_ORDER_APPENDED;
        return ApiResponseDto.success(code, result.order());
    }
}
