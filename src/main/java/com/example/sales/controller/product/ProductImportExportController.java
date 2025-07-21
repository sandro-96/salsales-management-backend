// File: src/main/java/com/example/sales/controller/product/ProductImportExportController.java
package com.example.sales.controller.product;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.security.RequireRole;
import com.example.sales.service.ExcelExportService;
import com.example.sales.service.ProductImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/products/import-export")
@RequiredArgsConstructor
public class ProductImportExportController {

    private final ProductImportService productImportService;
    private final ExcelExportService excelExportService;

    @Operation(summary = "Nhập sản phẩm từ file Excel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nhập sản phẩm thành công",
                    content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "File không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động")
    })
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireRole(ShopRole.OWNER)
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> importExcel(
            @Parameter(description = "ID cửa hàng", required = true)
            @RequestParam String shopId,

            @Parameter(description = "ID chi nhánh (tùy chọn)")
            @RequestParam(required = false) String branchId,

            @Parameter(description = "File Excel chứa danh sách sản phẩm", required = true)
            @RequestParam("file") MultipartFile file) {

        Map<String, Object> result = productImportService.importExcel(shopId, branchId, file);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_IMPORTED, result));
    }

    @Operation(summary = "Xuất danh sách sản phẩm ra file Excel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xuất file Excel thành công",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "500", description = "Lỗi xử lý file")
    })
    @GetMapping("/export")
    @RequireRole(ShopRole.OWNER)
    public ResponseEntity<byte[]> exportExcel(
            @Parameter(description = "ID cửa hàng", required = true)
            @RequestParam String shopId,

            @Parameter(description = "ID chi nhánh (tùy chọn)")
            @RequestParam(required = false) String branchId) throws IOException {

        return excelExportService.exportProducts(shopId, branchId);
    }
}
