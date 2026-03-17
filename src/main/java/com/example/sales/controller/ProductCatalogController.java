package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.Permission;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.product.ProductCatalogResponse;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.security.RequirePermission;
import com.example.sales.service.ProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@Tag(name = "Product Catalog", description = "Internal catalog tra cứu thông tin sản phẩm theo barcode")
public class ProductCatalogController {

    private final ProductCatalogService productCatalogService;

    /**
     * Tra cứu thông tin sản phẩm từ internal catalog theo barcode.
     *
     * Dùng khi người dùng quét / nhập barcode vào form tạo sản phẩm:
     * nếu barcode đã từng được shop nào đó trong hệ thống lưu trước đó,
     * endpoint này trả về name/category/description/images gợi ý để pre-fill form.
     *
     * HTTP 404 nếu barcode chưa có trong catalog (shop cần nhập thủ công lần đầu).
     */
    @GetMapping("/barcode/{barcode}")
    @Operation(
            summary = "Tra cứu thông tin sản phẩm theo barcode từ internal catalog",
            description = "Trả về thông tin gợi ý (name, category, description, images) để pre-fill form tạo sản phẩm. " +
                    "Catalog được tích lũy tự động mỗi khi bất kỳ shop nào lưu sản phẩm có barcode."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tìm thấy thông tin catalog cho barcode này"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực"),
            @ApiResponse(responseCode = "403", description = "Không có quyền"),
            @ApiResponse(responseCode = "404", description = "Barcode chưa có trong catalog")
    })
    public ApiResponseDto<ProductCatalogResponse> getByBarcode(
            @Parameter(description = "Mã barcode cần tra cứu") @PathVariable String barcode) {

        return productCatalogService.findByBarcode(barcode)
                .map(r -> ApiResponseDto.success(ApiCode.CATALOG_FOUND, r))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.CATALOG_NOT_FOUND));
    }
}

