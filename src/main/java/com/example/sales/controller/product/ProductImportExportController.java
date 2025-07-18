// File: main/java/com/example/sales/controller/product/ProductImportExportController.java
package com.example.sales.controller.product;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponse;
import com.example.sales.security.RequireRole;
import com.example.sales.service.ExcelExportService;
import com.example.sales.service.ProductImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Controller xử lý nhập/xuất sản phẩm (Excel, CSV).
 */
@RestController
@RequestMapping("/api/products/import-export")
@RequiredArgsConstructor
public class ProductImportExportController {
    private final ProductImportService productImportService;
    private final ExcelExportService excelExportService;

    /**
     * Nhập sản phẩm từ file Excel.
     *
     * @param shopId ID cửa hàng
     * @param file   File Excel chứa dữ liệu sản phẩm
     * @return Kết quả nhập
     */
    @PostMapping("/import")
    @RequireRole(ShopRole.OWNER)
    public ResponseEntity<ApiResponse<Map<String, Object>>> importExcel(
            @RequestParam String shopId,
            @RequestParam(required = false) String branchId,
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = productImportService.importExcel(shopId, branchId, file);
        return ResponseEntity.ok(ApiResponse.success(ApiCode.PRODUCT_IMPORTED, result));
    }
}
