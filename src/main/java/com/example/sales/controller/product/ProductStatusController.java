// File: src/main/java/com/example/sales/controller/product/ProductStatusController.java
package com.example.sales.controller.product;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.Permission;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequirePermission;
import com.example.sales.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý trạng thái active của sản phẩm.
 *
 * Có 2 tầng toggle tách biệt:
 *   - Shop level  : PATCH /shops/{shopId}/products/{productId}/toggle
 *                   Toggle Product.active — ảnh hưởng toàn bộ chi nhánh
 *   - Branch level: PATCH /shops/{shopId}/branches/{branchId}/products/{branchProductId}/toggle
 *                   Toggle BranchProduct.activeInBranch — chỉ ảnh hưởng chi nhánh đó
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductStatusController {

    private final ProductService productService;

    @Operation(
            summary = "Bật/tắt trạng thái kinh doanh của sản phẩm ở cấp shop",
            description = """
                    Toggle Product.active ở cấp shop.
                    - Khi TẮT (false): sản phẩm ngừng kinh doanh hoàn toàn — tất cả BranchProduct sẽ bị tắt activeInBranch theo.
                    - Khi BẬT (true): chỉ bật lại Product.active, activeInBranch tại từng chi nhánh KHÔNG tự động bật — cần bật lại thủ công từng chi nhánh.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật trạng thái thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện")
    })
    @PatchMapping("/shops/{shopId}/products/{productId}/toggle")
    @RequirePermission(Permission.PRODUCT_UPDATE_STATUS)
    public ResponseEntity<ApiResponseDto<ProductResponse>> toggleActiveShop(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID sản phẩm (Product ID)") @PathVariable String productId) {
        ProductResponse response = productService.toggleActiveShop(user.getId(), shopId, productId);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_STATUS_UPDATED, response));
    }

    @Operation(
            summary = "Bật/tắt trạng thái bán hàng của sản phẩm tại một chi nhánh",
            description = """
                    Toggle BranchProduct.activeInBranch — chỉ ảnh hưởng chi nhánh được chỉ định.
                    Lưu ý: Không thể bật lại tại chi nhánh nếu Product.active ở cấp shop đang là false.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật trạng thái thành công"),
            @ApiResponse(responseCode = "400", description = "Product đang bị tắt ở cấp shop"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện")
    })
    @PatchMapping("/shops/{shopId}/branches/{branchId}/products/{branchProductId}/toggle")
    @RequirePermission(Permission.PRODUCT_UPDATE_STATUS)
    public ResponseEntity<ApiResponseDto<ProductResponse>> toggleActiveInBranch(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID chi nhánh") @PathVariable String branchId,
            @Parameter(description = "ID BranchProduct") @PathVariable String branchProductId) {
        ProductResponse response = productService.toggleActiveInBranch(user.getId(), shopId, branchId, branchProductId);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_STATUS_UPDATED, response));
    }

    @Operation(
            summary = "Lấy danh sách sản phẩm tồn kho thấp",
            description = "Trả về danh sách BranchProduct có quantity < threshold. Có thể lọc theo chi nhánh cụ thể hoặc toàn shop."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Danh sách sản phẩm tồn kho thấp"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    @GetMapping("/shops/{shopId}/products/low-stock")
    @RequirePermission(Permission.PRODUCT_VIEW_LOW_STOCK)
    public ResponseEntity<ApiResponseDto<List<ProductResponse>>> getLowStock(
            @Parameter(description = "ID cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID chi nhánh (tùy chọn)") @RequestParam(required = false) String branchId,
            @Parameter(description = "Ngưỡng tồn kho thấp, mặc định = 10") @RequestParam(defaultValue = "10") int threshold) {
        List<ProductResponse> response = productService.getLowStockProducts(shopId, branchId, threshold);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_LOW_STOCK, response));
    }
}