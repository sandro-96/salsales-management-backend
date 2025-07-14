// File: src/main/java/com/example/sales/controller/ProductController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.product.ProductRequest;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.dto.product.ProductSearchRequest;
import com.example.sales.model.Product;
import com.example.sales.model.User;
import com.example.sales.service.ExcelExportService;
import com.example.sales.service.FileUploadService;
import com.example.sales.service.ProductImportService;
import com.example.sales.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;
    private final ExcelExportService excelExportService;
    private final ProductImportService productImportService;
    private final FileUploadService fileUploadService;

    @GetMapping
    public ApiResponse<List<ProductResponse>> getAllByUser(@AuthenticationPrincipal User user) {
        return ApiResponse.success(ApiCode.SUCCESS, productService.getAllByUser(user));
    }

    @PostMapping
    public ApiResponse<ProductResponse> createProduct(@AuthenticationPrincipal User user,
                                                      @RequestBody @Valid ProductRequest request) {
        return ApiResponse.success(ApiCode.PRODUCT_CREATED, productService.createProduct(user, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(@AuthenticationPrincipal User user,
                                                      @PathVariable String id,
                                                      @RequestBody @Valid ProductRequest request) {
        return ApiResponse.success(ApiCode.PRODUCT_UPDATED, productService.updateProduct(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteProduct(@AuthenticationPrincipal User user,
                                        @PathVariable String id) {
        productService.deleteProduct(user, id);
        return ApiResponse.success(ApiCode.SUCCESS);
    }

    @PostMapping("/search")
    public ApiResponse<Page<ProductResponse>> searchProducts(@AuthenticationPrincipal User user,
                                                             @RequestBody ProductSearchRequest req) {
        return ApiResponse.success(ApiCode.SUCCESS, productService.search(user, req));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportProducts(@AuthenticationPrincipal User user,
                                                 @ModelAttribute ProductSearchRequest req) {
        List<Product> products = productService.searchAllForExport(user, req);

        List<String> headers = List.of("Tên", "Danh mục", "Số lượng", "Đơn giá", "Đơn vị", "Trạng thái");

        Function<Product, List<String>> mapper = p -> List.of(
                p.getName(),
                p.getCategory(),
                String.valueOf(p.getQuantity()),
                String.valueOf(p.getPrice()),
                p.getUnit() != null ? p.getUnit() : "",
                p.isActive() ? "Đang bán" : "Ngưng bán"
        );

        return excelExportService.exportExcel(
                "danh_sach_san_pham.xlsx",
                "Products",
                headers,
                products,
                mapper
        );
    }

    @PutMapping("/{id}/toggle-active")
    public ApiResponse<ProductResponse> toggleActive(@AuthenticationPrincipal User user,
                                                     @PathVariable String id) {
        ProductResponse result = productService.toggleActive(user, id);
        return ApiResponse.success(ApiCode.PRODUCT_UPDATED, result);
    }

    @GetMapping("/low-stock")
    public ApiResponse<List<ProductResponse>> getLowStock(@AuthenticationPrincipal User user,
                                                          @RequestParam(defaultValue = "5") int threshold) {
        List<ProductResponse> results = productService.getLowStock(user, threshold);
        return ApiResponse.success(ApiCode.SUCCESS, results);
    }

    @PostMapping("/import")
    public ApiResponse<Map<String, Object>> importExcel(@AuthenticationPrincipal User user,
                                                        @RequestParam("file") MultipartFile file,
                                                        @RequestParam(required = false) String branchId) {
        Map<String, Object> result = productImportService.importExcel(user, branchId, file);
        return ApiResponse.success(ApiCode.SUCCESS, result);
    }

    @PostMapping("/upload-image")
    public ApiResponse<String> uploadImage(@RequestParam("file") MultipartFile file) {
        String imageUrl = fileUploadService.upload(file);
        return ApiResponse.success(ApiCode.SUCCESS, imageUrl);
    }
}
