// File: src/main/java/com/example/sales/controller/ProductController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.constant.ShopType;
import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.product.ProductRequest;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.dto.product.ProductSearchRequest;
import com.example.sales.model.Product;
import com.example.sales.model.User;
import com.example.sales.security.RequirePlan;
import com.example.sales.security.RequireRole;
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
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<List<ProductResponse>> getAllByUser(@RequestParam String shopId) {
        return ApiResponse.success(ApiCode.SUCCESS, productService.getAllByShop(shopId));
    }

    @PostMapping
    @RequireRole(ShopRole.OWNER)
    public ApiResponse<ProductResponse> createProduct(@RequestParam String shopId,
                                                      @RequestBody @Valid ProductRequest request) {
        return ApiResponse.success(ApiCode.PRODUCT_CREATED, productService.createProduct(shopId, request));
    }

    @PutMapping("/{id}")
    @RequireRole(ShopRole.OWNER)
    public ApiResponse<ProductResponse> updateProduct(@AuthenticationPrincipal User user,
                                                      @RequestParam String shopId,
                                                      @RequestParam ShopType shopType,
                                                      @PathVariable String id,
                                                      @RequestBody @Valid ProductRequest request) {
        return ApiResponse.success(ApiCode.PRODUCT_UPDATED, productService.updateProduct(user, shopId, shopType, id, request));
    }

    @DeleteMapping("/{id}")
    @RequireRole(ShopRole.OWNER)
    public ApiResponse<?> deleteProduct(@RequestParam String shopId,
                                        @PathVariable String id) {
        productService.deleteProduct(shopId, id);
        return ApiResponse.success(ApiCode.SUCCESS);
    }

    @PostMapping("/search")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<Page<ProductResponse>> searchProducts(@RequestParam String shopId,
                                                             @RequestBody ProductSearchRequest req) {
        return ApiResponse.success(ApiCode.SUCCESS, productService.search(shopId, req));
    }

    @GetMapping("/export")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ResponseEntity<byte[]> exportProducts(@RequestParam String shopId,
                                                 @ModelAttribute ProductSearchRequest req) {
        List<Product> products = productService.searchAllForExport(shopId, req);

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
    @RequireRole(ShopRole.OWNER)
    public ApiResponse<ProductResponse> toggleActive(@RequestParam String shopId,
                                                     @PathVariable String id) {
        return ApiResponse.success(ApiCode.PRODUCT_UPDATED, productService.toggleActive(shopId, id));
    }

    @RequirePlan({SubscriptionPlan.ENTERPRISE})
    @GetMapping("/low-stock")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<List<ProductResponse>> getLowStock(@RequestParam String shopId,
                                                          @RequestParam ShopType shopType,
                                                          @RequestParam(defaultValue = "5") int threshold) {
        List<ProductResponse> results = productService.getLowStock(shopId, threshold, shopType);
        return ApiResponse.success(ApiCode.SUCCESS, results);
    }

    @PostMapping("/import")
    @RequireRole(ShopRole.OWNER)
    public ApiResponse<Map<String, Object>> importExcel(@AuthenticationPrincipal User user,
                                                        @RequestParam("file") MultipartFile file,
                                                        @RequestParam String shopId,
                                                        @RequestParam(required = false) String branchId) {
        Map<String, Object> result = productImportService.importExcel(shopId, branchId, file);
        return ApiResponse.success(ApiCode.SUCCESS, result);
    }

    @PostMapping("/upload-image")
    public ApiResponse<String> uploadImage(@RequestParam("file") MultipartFile file) {
        String imageUrl = fileUploadService.upload(file);
        return ApiResponse.success(ApiCode.SUCCESS, imageUrl);
    }
}
