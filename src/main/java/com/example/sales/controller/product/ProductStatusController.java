// File: src/main/java/com/example/sales/controller/product/ProductStatusController.java
package com.example.sales.controller.product;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.security.RequireRole;
import com.example.sales.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
     * Bật/tắt trạng thái hoạt động của sản phẩm.
     */
    @Operation(summary = "Bật/tắt trạng thái hoạt động của sản phẩm")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật trạng thái thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện")
    })
    @PatchMapping("/{productId}/toggle")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ResponseEntity<ApiResponseDto<ProductResponse>> toggleActive(
            @Parameter(description = "ID cửa hàng", required = true)
            @RequestParam String shopId,

            @Parameter(description = "ID sản phẩm", required = true)
            @PathVariable String productId) {

        ProductResponse response = productService.toggleActive(shopId, productId);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_STATUS_UPDATED, response));
    }

    /**
     * Lấy danh sách sản phẩm có tồn kho dưới ngưỡng cảnh báo.
     */
    @Operation(summary = "Lấy danh sách sản phẩm tồn kho thấp")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Danh sách sản phẩm được trả về thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    @GetMapping("/low-stock")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ResponseEntity<ApiResponseDto<List<ProductResponse>>> getLowStock(
            @Parameter(description = "ID cửa hàng", required = true)
            @RequestParam String shopId,

            @Parameter(description = "Ngưỡng tồn kho thấp. Mặc định là 10", example = "10")
            @RequestParam(defaultValue = "10") int threshold) {

        List<ProductResponse> response = productService.getLowStockProducts(shopId, threshold);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_LOW_STOCK, response));
    }
}
