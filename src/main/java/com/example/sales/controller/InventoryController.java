// File: src/main/java/com/example/sales/controller/InventoryController.java
package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.InventoryType;
import com.example.sales.constant.Permission;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.inventory.InventoryRequest;
import com.example.sales.dto.inventory.InventoryTransactionResponse;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequirePermission;
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
@RequestMapping("/api/shops/{shopId}/inventory") // Thêm shopId vào base path
@RequiredArgsConstructor
@Validated
public class InventoryController {

    private final InventoryService inventoryService;

    // ✅ Endpoint để nhập sản phẩm vào kho
    @PostMapping("/import")
    @RequirePermission(Permission.INVENTORY_MANAGE)
    @Operation(summary = "Nhập thêm số lượng sản phẩm vào kho của chi nhánh", description = "Tạo một giao dịch nhập kho cho sản phẩm tại một chi nhánh cụ thể.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nhập kho thành công, trả về số lượng tồn kho mới"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Cửa hàng, chi nhánh hoặc sản phẩm không tìm thấy")
    })
    public ApiResponseDto<Integer> importProduct(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID của cửa hàng") @PathVariable String shopId,
            @RequestBody @Valid @Parameter(description = "Thông tin nhập kho") InventoryRequest request) {

        // Kiểm tra loại giao dịch phải là IMPORT
        if (request.getType() != InventoryType.IMPORT) {
            throw new IllegalArgumentException("Loại giao dịch phải là 'IMPORT' cho endpoint này.");
        }

        int newQuantity = inventoryService.importProductQuantity(
                user.getId(), shopId, request.getBranchId(), request.getBranchProductId(),
                request.getQuantity(), request.getNote());

        return ApiResponseDto.success(ApiCode.SUCCESS, newQuantity);
    }

    // ✅ Endpoint để xuất sản phẩm khỏi kho
    @PostMapping("/export")
    @RequirePermission(Permission.INVENTORY_MANAGE)
    @Operation(summary = "Xuất bớt số lượng sản phẩm khỏi kho của chi nhánh", description = "Tạo một giao dịch xuất kho cho sản phẩm tại một chi nhánh cụ thể. Kiểm tra số lượng tồn kho.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xuất kho thành công, trả về số lượng tồn kho mới"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ hoặc không đủ tồn kho"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Cửa hàng, chi nhánh hoặc sản phẩm không tìm thấy")
    })
    public ApiResponseDto<Integer> exportProduct(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID của cửa hàng") @PathVariable String shopId,
            @RequestBody @Valid @Parameter(description = "Thông tin xuất kho") InventoryRequest request) {

        // Kiểm tra loại giao dịch phải là EXPORT
        if (request.getType() != InventoryType.EXPORT) {
            throw new IllegalArgumentException("Loại giao dịch phải là 'EXPORT' cho endpoint này.");
        }

        int newQuantity = inventoryService.exportProductQuantity(
                user.getId(), shopId, request.getBranchId(), request.getBranchProductId(),
                request.getQuantity(), request.getNote(), request.getReferenceId());

        return ApiResponseDto.success(ApiCode.SUCCESS, newQuantity);
    }

    // ✅ Endpoint để điều chỉnh tồn kho
    @PostMapping("/adjust")
    @RequirePermission(Permission.INVENTORY_MANAGE)
    @Operation(summary = "Điều chỉnh số lượng tồn kho của sản phẩm tại chi nhánh", description = "Tạo một giao dịch điều chỉnh tồn kho cho sản phẩm tại một chi nhánh cụ thể. Có thể tăng hoặc giảm.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Điều chỉnh tồn kho thành công, trả về số lượng tồn kho mới"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Cửa hàng, chi nhánh hoặc sản phẩm không tìm thấy")
    })
    public ApiResponseDto<Integer> adjustProduct(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID của cửa hàng") @PathVariable String shopId,
            @RequestBody @Valid @Parameter(description = "Thông tin điều chỉnh tồn kho") InventoryRequest request) {

        // Kiểm tra loại giao dịch phải là ADJUSTMENT
        if (request.getType() != InventoryType.ADJUSTMENT) {
            throw new IllegalArgumentException("Loại giao dịch phải là 'ADJUSTMENT' cho endpoint này.");
        }

        // Đối với ADJUSTMENT, request.getQuantity() sẽ là newQuantity
        int newQuantity = inventoryService.adjustProductQuantity(
                user.getId(), shopId, request.getBranchId(), request.getBranchProductId(),
                request.getQuantity(), request.getNote());

        return ApiResponseDto.success(ApiCode.SUCCESS, newQuantity);
    }

    // ✅ Endpoint để lấy lịch sử giao dịch tồn kho
    @GetMapping("/branches/{branchId}/products/{branchProductId}/history")
    @RequirePermission(Permission.INVENTORY_VIEW)
    @Operation(summary = "Lấy lịch sử giao dịch tồn kho cho một sản phẩm tại chi nhánh", description = "Lấy lịch sử giao dịch tồn kho của một BranchProduct cụ thể với phân trang.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lịch sử giao dịch tồn kho được trả về thành công"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Sản phẩm không tìm thấy")
    })
    public ApiResponseDto<Page<InventoryTransactionResponse>> getHistory(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @Parameter(description = "ID của cửa hàng") @PathVariable String shopId,
            @Parameter(description = "ID của chi nhánh") @PathVariable String branchId,
            @Parameter(description = "ID của BranchProduct") @PathVariable String branchProductId,
            @Parameter(description = "Thông tin phân trang (page, size, sort)") Pageable pageable) {

        // Bạn có thể thêm kiểm tra quyền chi tiết hơn ở đây nếu cần,
        // ví dụ: đảm bảo user có quyền trong branchId này.
        // Hiện tại, @RequireRole sẽ kiểm tra quyền ở cấp shop.

        Page<InventoryTransactionResponse> history = inventoryService.getTransactionHistory(user.getId(), shopId, branchId, branchProductId, pageable);
        return ApiResponseDto.success(ApiCode.SUCCESS, history);
    }
}