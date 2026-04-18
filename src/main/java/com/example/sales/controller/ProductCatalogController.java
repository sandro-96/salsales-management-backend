package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.product.ProductCatalogResponse;
import com.example.sales.exception.ResourceNotFoundException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@Tag(name = "Product Catalog", description = "Catalog chuẩn — tra cứu theo barcode hoặc tìm theo tên")
public class ProductCatalogController {

    private final ProductCatalogService productCatalogService;

    @GetMapping("/search")
    @Operation(
            summary = "Tìm kiếm catalog chuẩn theo tên (gợi ý khi tạo sản phẩm)",
            description = "Substring không phân biệt hoa thường; keyword tối thiểu 2 ký tự."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Danh sách gợi ý (có thể rỗng)"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    public ApiResponseDto<List<ProductCatalogResponse>> searchByName(
            @Parameter(description = "Từ khoá tên sản phẩm") @RequestParam String keyword,
            @Parameter(description = "Số bản ghi tối đa (1–50, mặc định 8)") @RequestParam(defaultValue = "8") int size) {

        List<ProductCatalogResponse> list = productCatalogService.searchByNameKeyword(keyword, size);
        return ApiResponseDto.success(ApiCode.CATALOG_SEARCH_OK, list);
    }

    /**
     * Tra cứu thông tin sản phẩm từ catalog chuẩn (do system admin duy trì) theo barcode.
     *
     * HTTP 404 nếu barcode chưa có trong catalog.
     */
    @GetMapping("/barcode/{barcode}")
    @Operation(
            summary = "Tra cứu thông tin sản phẩm theo barcode từ catalog chuẩn",
            description = "Trả về name/category/description/images gợi ý để pre-fill form tạo sản phẩm. " +
                    "Nội dung catalog chỉ được tạo/cập nhật qua system admin (PUT /api/admin/catalog)."
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

