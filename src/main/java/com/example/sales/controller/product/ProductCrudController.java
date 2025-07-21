// File: src/main/java/com/example/sales/controller/product/ProductCrudController.java
package com.example.sales.controller.product;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.product.ProductRequest;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequireRole;
import com.example.sales.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api") // Base path for more granular control with shopId/branchId
@RequiredArgsConstructor
public class ProductCrudController {

    private final ProductService productService;

    @Operation(summary = "Tạo sản phẩm mới tại một cửa hàng và chi nhánh cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo sản phẩm thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền tạo")
    })
    @PostMapping("/shops/{shopId}/products")
    @RequireRole(ShopRole.OWNER)
    public ResponseEntity<ApiResponseDto<ProductResponse>> create(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(shopId, request);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_CREATED, response));
    }

    @Operation(summary = "Cập nhật sản phẩm tại một chi nhánh cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật sản phẩm thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm"),
            @ApiResponse(responseCode = "403", description = "Không có quyền cập nhật")
    })
    @PutMapping("/shops/{shopId}/branches/{branchId}/products/{id}")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ResponseEntity<ApiResponseDto<ProductResponse>> update(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID chi nhánh") @PathVariable String branchId,
            @Parameter(description = "ID sản phẩm (BranchProduct ID)") @PathVariable String id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(user.getId(), shopId, branchId, id, request);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_UPDATED, response));
    }

    @Operation(summary = "Xóa sản phẩm (xóa mềm) tại một chi nhánh cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm"),
            @ApiResponse(responseCode = "403", description = "Không có quyền xóa")
    })
    @DeleteMapping("/shops/{shopId}/branches/{branchId}/products/{id}")
    @RequireRole(ShopRole.OWNER)
    public ResponseEntity<ApiResponseDto<Void>> delete(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID chi nhánh") @PathVariable String branchId,
            @Parameter(description = "ID sản phẩm (BranchProduct ID)") @PathVariable String id) {
        productService.deleteProduct(user.getId(), shopId, branchId, id);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_DELETED, null));
    }

    @Operation(summary = "Lấy danh sách sản phẩm với phân trang cho một cửa hàng hoặc chi nhánh")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách sản phẩm")
    })
    @GetMapping("/shops/{shopId}/products")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ResponseEntity<ApiResponseDto<Page<ProductResponse>>> getAll(
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID chi nhánh (tùy chọn)") @RequestParam(required = false) String branchId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<ProductResponse> response = productService.getAllByShop(shopId, branchId, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_LIST, response));
    }

    @Operation(summary = "Lấy chi tiết sản phẩm tại một chi nhánh cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về chi tiết sản phẩm"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm")
    })
    @GetMapping("/shops/{shopId}/branches/{branchId}/products/{id}")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ResponseEntity<ApiResponseDto<ProductResponse>> getById(
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID chi nhánh") @PathVariable String branchId,
            @Parameter(description = "ID sản phẩm (BranchProduct ID)") @PathVariable String id) {
        ProductResponse response = productService.getProduct(shopId, branchId, id);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_FOUND, response));
    }

    @Operation(summary = "Tìm kiếm sản phẩm theo từ khóa cho một cửa hàng hoặc chi nhánh")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách sản phẩm khớp từ khóa")
    })
    @GetMapping("/shops/{shopId}/products/search")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ResponseEntity<ApiResponseDto<Page<ProductResponse>>> search(
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID chi nhánh (tùy chọn)") @RequestParam(required = false) String branchId,
            @Parameter(description = "Từ khóa tìm kiếm") @RequestParam String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<ProductResponse> response = productService.searchProducts(shopId, branchId, keyword, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_SEARCH_RESULTS, response));
    }
}