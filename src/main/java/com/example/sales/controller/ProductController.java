// File: src/main/java/com/example/sales/controller/ProductController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiMessage;
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
import com.example.sales.util.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;
    private final MessageService messageService;
    private final ExcelExportService excelExportService;
    private final ProductImportService productImportService;
    private final FileUploadService fileUploadService;

    // Trả về List<ProductResponse> thay vì List<Product>
    @GetMapping
    public ApiResponse<List<ProductResponse>> getAllByUser(@AuthenticationPrincipal User user, Locale locale) {
        return ApiResponse.success(ApiMessage.PRODUCT_LIST, productService.getAllByUser(user), messageService, locale);
    }

    @PostMapping
    public ApiResponse<ProductResponse> createProduct(@AuthenticationPrincipal User user,
                                                      @RequestBody @Valid ProductRequest request,
                                                      Locale locale) {
        return ApiResponse.success(ApiMessage.PRODUCT_CREATED, productService.createProduct(user, request), messageService, locale);
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(@AuthenticationPrincipal User user,
                                                      @PathVariable String id,
                                                      @RequestBody @Valid ProductRequest request,
                                                      Locale locale) {
        return ApiResponse.success(ApiMessage.PRODUCT_UPDATED, productService.updateProduct(user, id, request), messageService, locale);
    }


    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteProduct(@AuthenticationPrincipal User user,
                                        @PathVariable String id,
                                        Locale locale) {
        productService.deleteProduct(user, id);
        return ApiResponse.success(ApiMessage.PRODUCT_DELETED, messageService, locale);
    }

    @PostMapping("/search")
    public ApiResponse<Page<ProductResponse>> searchProducts(@AuthenticationPrincipal User user,
                                                             @RequestBody ProductSearchRequest req,
                                                             Locale locale) {
        return ApiResponse.success(ApiMessage.PRODUCT_LIST, productService.search(user, req), messageService, locale);
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
                                                     @PathVariable String id,
                                                     Locale locale) {
        ProductResponse result = productService.toggleActive(user, id);
        return ApiResponse.success(ApiMessage.PRODUCT_UPDATED, result, messageService, locale);
    }

    @GetMapping("/low-stock")
    public ApiResponse<List<ProductResponse>> getLowStock(@AuthenticationPrincipal User user,
                                                          @RequestParam(defaultValue = "5") int threshold,
                                                          Locale locale) {
        List<ProductResponse> results = productService.getLowStock(user, threshold);
        return ApiResponse.success(ApiMessage.PRODUCT_LIST, results, messageService, locale);
    }

    @PostMapping("/import")
    public ApiResponse<Map<String, Object>> importExcel(@AuthenticationPrincipal User user,
                                                        @RequestParam("file") MultipartFile file,
                                                        @RequestParam(required = false) String branchId,
                                                        Locale locale) {
        Map<String, Object> result = productImportService.importExcel(user, branchId, file);
        return ApiResponse.success(ApiMessage.PRODUCT_IMPORT_SUCCESS, result, messageService, locale);
    }

    @PostMapping("/upload-image")
    public ApiResponse<String> uploadImage(@RequestParam("file") MultipartFile file, Locale locale) {
        String imageUrl = fileUploadService.upload(file);
        return ApiResponse.success(ApiMessage.PRODUCT_UPLOAD_IMAGE_SUCCESS, imageUrl, messageService, locale);
    }

}

