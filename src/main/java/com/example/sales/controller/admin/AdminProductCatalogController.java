package com.example.sales.controller.admin;

import com.example.sales.constant.AdminPermission;
import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.product.ProductCatalogResponse;
import com.example.sales.dto.product.ProductCatalogUpsertRequest;
import com.example.sales.security.Audited;
import com.example.sales.security.RequireAdminPermission;
import com.example.sales.service.ProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản lý catalog sản phẩm chuẩn hoá — chỉ admin có {@code CATALOG_MANAGE}.
 * Shop chỉ tra cứu qua {@code GET /api/catalog/barcode/{barcode}}.
 */
@RestController
@RequestMapping("/api/admin/catalog")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin — Product catalog", description = "CRUD catalog chuẩn (system admin)")
public class AdminProductCatalogController {

    private final ProductCatalogService productCatalogService;

    @GetMapping
    @Operation(summary = "Liệt kê catalog với paging + filter theo keyword/category")
    @RequireAdminPermission(AdminPermission.CATALOG_MANAGE)
    public ApiResponseDto<Page<ProductCatalogResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                productCatalogService.list(keyword, category, pageable));
    }

    @PutMapping
    @Operation(summary = "Tạo hoặc cập nhật một mục catalog theo barcode")
    @RequireAdminPermission(AdminPermission.CATALOG_MANAGE)
    @Audited(resource = "CATALOG", action = "UPSERT", targetLabelExpr = "#request.barcode")
    public ApiResponseDto<ProductCatalogResponse> upsert(@Valid @RequestBody ProductCatalogUpsertRequest request) {
        ProductCatalogResponse saved = productCatalogService.upsertFromAdmin(request);
        return ApiResponseDto.success(ApiCode.CATALOG_SAVED, saved);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá một mục catalog theo id")
    @RequireAdminPermission(AdminPermission.CATALOG_MANAGE)
    @Audited(resource = "CATALOG", action = "DELETE", targetIdExpr = "#id")
    public ApiResponseDto<Void> delete(@PathVariable String id) {
        productCatalogService.deleteById(id);
        return ApiResponseDto.success(ApiCode.CATALOG_DELETED, null);
    }
}
