// File: src/main/java/com/example/sales/controller/InventoryController.java
package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.inventory.InventoryRequest;
import com.example.sales.model.InventoryTransaction;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequireRole;
import com.example.sales.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Validated
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @RequireRole(ShopRole.OWNER)
    @Operation(summary = "Tạo giao dịch tồn kho", description = "Tạo một giao dịch nhập hoặc xuất kho cho sản phẩm")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Giao dịch tồn kho được tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ hoặc sản phẩm không đủ tồn kho"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Cửa hàng hoặc sản phẩm không tìm thấy")
    })
    public ApiResponseDto<InventoryTransaction> create(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestBody @Valid @Parameter(description = "Thông tin giao dịch tồn kho") InventoryRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS, inventoryService.createTransaction(user.getId(), shopId, request));
    }

    @GetMapping("/history")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    @Operation(summary = "Lấy lịch sử giao dịch tồn kho", description = "Lấy lịch sử giao dịch tồn kho của sản phẩm với phân trang")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lịch sử giao dịch tồn kho được trả về thành công"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Sản phẩm không tìm thấy")
    })
    public ApiResponseDto<Page<InventoryTransaction>> getHistory(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của sản phẩm") String productId,
            @Parameter(description = "Thông tin phân trang (page, size, sort)") Pageable pageable) {
        return ApiResponseDto.success(ApiCode.SUCCESS, inventoryService.getHistory(user.getId(), productId, pageable));
    }
}
