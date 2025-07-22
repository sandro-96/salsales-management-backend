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
 * Controller xử lý trạng thái sản phẩm (bật/tắt, tồn kho thấp).
 */
@RestController
@RequestMapping("/api/products/status")
@RequiredArgsConstructor
public class ProductStatusController {

    private final ProductService productService;

    /**
     * Bật/tắt trạng thái hoạt động của sản phẩm tại một chi nhánh cụ thể.
     */
    @Operation(summary = "Bật/tắt trạng thái hoạt động của sản phẩm tại một chi nhánh")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật trạng thái thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện")
    })
    @PatchMapping("/shops/{shopId}/branches/{branchId}/products/{branchProductId}/toggle")
    @RequirePermission(Permission.PRODUCT_UPDATE_STATUS)
    public ResponseEntity<ApiResponseDto<ProductResponse>> toggleActive(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID cửa hàng", required = true) @PathVariable String shopId,
            @Parameter(description = "ID chi nhánh", required = true) @PathVariable String branchId,
            @Parameter(description = "ID sản phẩm (BranchProduct ID)", required = true) @PathVariable String branchProductId) {

        ProductResponse response = productService.toggleActive(user.getId(), shopId, branchId, branchProductId);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_STATUS_UPDATED, response));
    }

    /**
     * Lấy danh sách sản phẩm có tồn kho dưới ngưỡng cảnh báo cho một cửa hàng hoặc chi nhánh.
     */
    @Operation(summary = "Lấy danh sách sản phẩm tồn kho thấp cho một cửa hàng hoặc chi nhánh")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Danh sách sản phẩm được trả về thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    @GetMapping("/shops/{shopId}/low-stock")
    @RequirePermission(Permission.PRODUCT_VIEW_LOW_STOCK)
    public ResponseEntity<ApiResponseDto<List<ProductResponse>>> getLowStock(
            @Parameter(description = "ID cửa hàng", required = true) @PathVariable String shopId,
            @Parameter(description = "ID chi nhánh (tùy chọn)") @RequestParam(required = false) String branchId,
            @Parameter(description = "Ngưỡng tồn kho thấp. Mặc định là 10", example = "10")
            @RequestParam(defaultValue = "10") int threshold) {

        List<ProductResponse> response = productService.getLowStockProducts(shopId, branchId, threshold);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_LOW_STOCK, response));
    }
}