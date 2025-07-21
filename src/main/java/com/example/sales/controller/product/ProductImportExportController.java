// File: src/main/java/com/example/sales/controller/product/ProductImportExportController.java
package com.example.sales.controller.product;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequireRole;
import com.example.sales.service.ExcelExportService;
import com.example.sales.service.ExcelImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/products/import-export")
@RequiredArgsConstructor
public class ProductImportExportController {

    private final ExcelImportService excelImportService;
    private final ExcelExportService excelExportService;

    @Operation(summary = "Nhập sản phẩm từ file Excel vào một chi nhánh cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nhập sản phẩm thành công"),
            @ApiResponse(responseCode = "400", description = "File không hợp lệ hoặc dữ liệu sai định dạng"),
            @ApiResponse(responseCode = "403", description = "Không có quyền nhập")
    })
    @PostMapping("/import")
    @RequireRole(ShopRole.OWNER)
    public ResponseEntity<ApiResponseDto<Integer>> importProducts(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID cửa hàng") @RequestParam String shopId,
            @Parameter(description = "ID chi nhánh mà sản phẩm sẽ được nhập vào") @RequestParam String branchId,
            @Parameter(description = "File Excel chứa dữ liệu sản phẩm") @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống.");
        }
        try {
            int importedCount = excelImportService.importProducts(shopId, branchId, file.getInputStream());
            return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_IMPORTED, importedCount));
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc file hoặc xử lý dữ liệu: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "Xuất sản phẩm ra file Excel cho một cửa hàng hoặc một chi nhánh cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xuất file Excel thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền xuất")
    })
    @GetMapping("/export")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public void exportProducts(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID cửa hàng") @RequestParam String shopId,
            @Parameter(description = "ID chi nhánh (tùy chọn). Nếu không cung cấp, sẽ xuất tất cả sản phẩm của shop trên mọi chi nhánh.")
            @RequestParam(required = false) String branchId,
            HttpServletResponse response) throws IOException {

        // ✅ Lấy ResponseEntity<byte[]> từ ExcelExportService
        ResponseEntity<byte[]> excelResponse = excelExportService.exportProducts(shopId, branchId);

        // ✅ Ghi dữ liệu từ ResponseEntity vào HttpServletResponse
        response.setContentType(excelResponse.getHeaders().getContentType().toString());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, excelResponse.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        response.setContentLength(excelResponse.getBody().length); // Đặt Content-Length
        response.getOutputStream().write(excelResponse.getBody());
        response.getOutputStream().flush(); // Đảm bảo tất cả dữ liệu được ghi
    }
}