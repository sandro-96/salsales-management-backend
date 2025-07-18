// File: main/java/com/example/sales/controller/product/ProductCrudController.java
package com.example.sales.controller.product;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.product.ProductRequest;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.security.RequireRole;
import com.example.sales.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductCrudController {
    private final ProductService productService;

    /**
     * Tạo sản phẩm mới.
     *
     * @param shopId  ID của cửa hàng
     * @param request Chi tiết sản phẩm
     * @return Phản hồi với sản phẩm đã tạo
     */
    @PostMapping
    @RequireRole(ShopRole.OWNER)
    public ResponseEntity<ApiResponseDto<ProductResponse>> create(
            @RequestParam String shopId,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(shopId, request);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_CREATED, response));
    }

    /**
     * Cập nhật sản phẩm.
     *
     * @param userId  ID người dùng
     * @param shopId  ID cửa hàng
     * @param id      ID sản phẩm
     * @param request Chi tiết sản phẩm cần cập nhật
     * @return Phản hồi với sản phẩm đã cập nhật
     */
    @PutMapping("/{id}")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ResponseEntity<ApiResponseDto<ProductResponse>> update(
            @RequestParam String userId,
            @RequestParam String shopId,
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(userId, shopId, id, request);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_UPDATED, response));
    }

    /**
     * Xóa sản phẩm (xóa mềm).
     *
     * @param shopId ID cửa hàng
     * @param id     ID sản phẩm
     * @return Phản hồi xác nhận xóa
     */
    @DeleteMapping("/{id}")
    @RequireRole(ShopRole.OWNER)
    public ResponseEntity<ApiResponseDto<Void>> delete(
            @RequestParam String shopId,
            @PathVariable String id) {
        productService.deleteProduct(shopId, id);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_DELETED, null));
    }

    /**
     * Lấy danh sách sản phẩm với phân trang.
     *
     * @param shopId   ID cửa hàng
     * @param pageable Thông tin phân trang
     * @return Danh sách sản phẩm
     */
    @GetMapping
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ResponseEntity<ApiResponseDto<Page<ProductResponse>>> getAll(
            @RequestParam String shopId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductResponse> response = productService.getAllByShop(shopId, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_LIST, response));
    }

    /**
     * Lấy chi tiết sản phẩm.
     *
     * @param shopId ID cửa hàng
     * @param id     ID sản phẩm
     * @return Chi tiết sản phẩm
     */
    @GetMapping("/{id}")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ResponseEntity<ApiResponseDto<ProductResponse>> getById(
            @RequestParam String shopId,
            @PathVariable String id) {
        ProductResponse response = productService.getProduct(shopId, id);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_FOUND, response));
    }

    /**
     * Lấy danh sách sản phẩm theo từ khóa tìm kiếm.
     *
     * @param shopId   ID cửa hàng
     * @param keyword  Từ khóa tìm kiếm
     * @param pageable Thông tin phân trang
     * @return Danh sách sản phẩm phù hợp với từ khóa
     */
    @GetMapping("/search")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ResponseEntity<ApiResponseDto<Page<ProductResponse>>> search(
            @RequestParam String shopId,
            @RequestParam String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductResponse> response = productService.searchProducts(shopId, keyword, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_SEARCH_RESULTS, response));
    }
}
