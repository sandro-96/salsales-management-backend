package com.example.sales.controller.admin;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.product.ProductCatalogUpsertRequest;
import com.example.sales.dto.product.ProductCatalogResponse;
import com.example.sales.service.ProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản lý catalog sản phẩm chuẩn hoá — chỉ user có role {@code ROLE_ADMIN}.
 * Shop chỉ tra cứu qua {@code GET /api/catalog/barcode/{barcode}}.
 */
@RestController
@RequestMapping("/api/admin/catalog")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin — Product catalog", description = "Tạo / cập nhật catalog chuẩn (system admin)")
public class AdminProductCatalogController {

    private final ProductCatalogService productCatalogService;

    @PutMapping
    @Operation(summary = "Tạo hoặc cập nhật một mục catalog theo barcode")
    public ApiResponseDto<ProductCatalogResponse> upsert(@Valid @RequestBody ProductCatalogUpsertRequest request) {
        ProductCatalogResponse saved = productCatalogService.upsertFromAdmin(request);
        return ApiResponseDto.success(ApiCode.CATALOG_SAVED, saved);
    }
}
