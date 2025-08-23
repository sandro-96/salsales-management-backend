package com.example.sales.controller.product;

import com.example.sales.cache.ProductCache;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.Permission;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.product.ProductRequest;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequirePermission;
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

import java.util.List;

/**
 * Controller xử lý CRUD cho sản phẩm tại shop và chi nhánh.
 * Hỗ trợ tạo sản phẩm từ shop (với tùy chọn branchIds) hoặc từ chi nhánh (tạo Product và BranchProduct).
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductCrudController {

    private final ProductService productService;
    private final ProductCache productCache;

    @Operation(summary = "Tạo sản phẩm mới tại một cửa hàng và các chi nhánh cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo sản phẩm thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền tạo")
    })
    @PostMapping("/shops/{shopId}/products")
    @RequirePermission(Permission.PRODUCT_CREATE)
    public ResponseEntity<ApiResponseDto<ProductResponse>> create(
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "Danh sách ID chi nhánh (tùy chọn)") @RequestParam(required = false) List<String> branchIds,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(shopId, branchIds, request);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_CREATED, response));
    }

    @Operation(summary = "Tạo sản phẩm mới từ chi nhánh (tạo Product cho shop và BranchProduct cho chi nhánh)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo sản phẩm thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền tạo"),
            @ApiResponse(responseCode = "404", description = "Chi nhánh không tồn tại")
    })
    @PostMapping("/shops/{shopId}/branches/{branchId}/products")
    @RequirePermission(Permission.PRODUCT_CREATE)
    public ResponseEntity<ApiResponseDto<ProductResponse>> createFromBranch(
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID chi nhánh") @PathVariable String branchId,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createBranchProduct(shopId, branchId, request);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_CREATED, response));
    }

    @Operation(summary = "Cập nhật sản phẩm tại một cửa hàng và các chi nhánh cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật sản phẩm thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền cập nhật"),
            @ApiResponse(responseCode = "404", description = "Sản phẩm hoặc chi nhánh không tồn tại")
    })
    @PutMapping("/shops/{shopId}/products/{id}")
    @RequirePermission(Permission.PRODUCT_UPDATE)
    public ResponseEntity<ApiResponseDto<ProductResponse>> update(
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID sản phẩm (BranchProduct ID)") @PathVariable String id,
            @Parameter(description = "Danh sách ID chi nhánh (tùy chọn)") @RequestParam(required = false) List<String> branchIds,
            @Valid @RequestBody ProductRequest request,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") String userId) {
        ProductResponse response = productService.updateProduct(userId, shopId, branchIds, id, request);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_UPDATED, response));
    }

    @Operation(summary = "Xóa sản phẩm (xóa mềm) tại một chi nhánh cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm"),
            @ApiResponse(responseCode = "403", description = "Không có quyền xóa")
    })
    @DeleteMapping("/shops/{shopId}/branches/{branchId}/products/{id}")
    @RequirePermission(Permission.PRODUCT_DELETE)
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
    public ResponseEntity<ApiResponseDto<Page<ProductResponse>>> getAll(
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID chi nhánh (tùy chọn)") @RequestParam(required = false) String branchId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductResponse> response = productCache.getAllByShop(shopId, branchId, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_LIST, response));
    }

    @Operation(summary = "Lấy chi tiết sản phẩm tại một chi nhánh cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về chi tiết sản phẩm"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm")
    })
    @GetMapping("/shops/{shopId}/branches/{branchId}/products/{id}")
    public ResponseEntity<ApiResponseDto<ProductResponse>> getById(
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID chi nhánh") @PathVariable String branchId,
            @Parameter(description = "ID sản phẩm (BranchProduct ID)") @PathVariable String id) {
        ProductResponse response = productService.getProduct(shopId, branchId, id);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_FOUND, response));
    }

    @Operation(summary = "Bật/tắt trạng thái active của sản phẩm tại chi nhánh")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật trạng thái thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm"),
            @ApiResponse(responseCode = "403", description = "Không có quyền cập nhật")
    })
    @PatchMapping("/shops/{shopId}/branches/{branchId}/products/{branchProductId}/toggle-active")
    @RequirePermission(Permission.PRODUCT_UPDATE)
    public ResponseEntity<ApiResponseDto<ProductResponse>> toggleActive(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID chi nhánh") @PathVariable String branchId,
            @Parameter(description = "ID BranchProduct") @PathVariable String branchProductId) {
        ProductResponse response = productService.toggleActive(user.getId(), shopId, branchId, branchProductId);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_UPDATED, response));
    }

    @Operation(summary = "Lấy mã gợi ý cho sản phẩm (SKU)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về mã gợi ý"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @GetMapping("/shops/{shopId}/suggested-sku")
    public ResponseEntity<ApiResponseDto<String>> getSuggestedSku(
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "Ngành hàng") @RequestParam String industry,
            @Parameter(description = "ID danh mục") @RequestParam String category) {
        String suggestedCode = productService.getSuggestedSku(shopId, industry, category);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.SUCCESS, suggestedCode));
    }

    @Operation(summary = "Lấy mã vạch gợi ý cho sản phẩm")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về mã vạch gợi ý"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @GetMapping("/shops/{shopId}/suggested-barcode")
    public ResponseEntity<ApiResponseDto<String>> getSuggestedBarcode(
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "Ngành hàng") @RequestParam String industry,
            @Parameter(description = "ID danh mục") @RequestParam String category) {
        String suggestedCode = productService.getSuggestedBarcode(shopId, industry, category);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.SUCCESS, suggestedCode));
    }
}