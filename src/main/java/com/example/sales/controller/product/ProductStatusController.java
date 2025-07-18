// File: main/java/com/example/sales/controller/product/ProductStatusController.java
package com.example.sales.controller.product;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.security.RequireRole;
import com.example.sales.service.ProductService;
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
     * Bật/tắt trạng thái sản phẩm.
     *
     * @param shopId    ID cửa hàng
     * @param productId ID sản phẩm
     * @return Phản hồi với sản phẩm đã cập nhật
     */
    @PatchMapping("/{productId}/toggle")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ResponseEntity<ApiResponseDto<ProductResponse>> toggleActive(
            @RequestParam String shopId,
            @PathVariable String productId) {
        ProductResponse response = productService.toggleActive(shopId, productId);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_STATUS_UPDATED, response));
    }

    /**
     * Lấy danh sách sản phẩm tồn kho thấp.
     *
     * @param shopId   ID cửa hàng
     * @param threshold Ngưỡng tồn kho thấp
     * @return Danh sách sản phẩm tồn kho thấp
     */
    @GetMapping("/low-stock")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ResponseEntity<ApiResponseDto<List<ProductResponse>>> getLowStock(
            @RequestParam String shopId,
            @RequestParam(defaultValue = "10") int threshold) {
        List<ProductResponse> response = productService.getLowStockProducts(shopId, threshold);
        return ResponseEntity.ok(ApiResponseDto.success(ApiCode.PRODUCT_LOW_STOCK, response));
    }
}
