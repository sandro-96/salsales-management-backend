// File: src/main/java/com/example/sales/controller/CustomerController.java
package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.customer.CustomerRequest;
import com.example.sales.dto.customer.CustomerResponse;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequireRole;
import com.example.sales.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Validated
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    @Operation(summary = "Lấy danh sách khách hàng", description = "Trả về danh sách khách hàng theo cửa hàng và (tuỳ chọn) chi nhánh")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách khách hàng thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ApiResponseDto<List<CustomerResponse>> getAll(
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestParam(required = false) @Parameter(description = "ID của chi nhánh (tuỳ chọn)") String branchId) {
        return ApiResponseDto.success(ApiCode.CUSTOMER_LIST, customerService.getCustomers(shopId, branchId));
    }

    @PostMapping
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    @Operation(summary = "Tạo khách hàng", description = "Tạo một khách hàng mới trong cửa hàng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo khách hàng thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ApiResponseDto<CustomerResponse> create(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestBody @Valid @Parameter(description = "Thông tin khách hàng") CustomerRequest request) {
        return ApiResponseDto.success(ApiCode.CUSTOMER_CREATED, customerService.createCustomer(shopId, user.getId(), request));
    }

    @PutMapping("/{id}")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    @Operation(summary = "Cập nhật khách hàng", description = "Cập nhật thông tin khách hàng theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật khách hàng thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy khách hàng")
    })
    public ApiResponseDto<CustomerResponse> update(
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @PathVariable @Parameter(description = "ID của khách hàng") String id,
            @RequestBody @Valid @Parameter(description = "Thông tin cập nhật khách hàng") CustomerRequest request) {
        return ApiResponseDto.success(ApiCode.CUSTOMER_UPDATED, customerService.updateCustomer(shopId, id, request));
    }

    @DeleteMapping("/{id}")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    @Operation(summary = "Xóa khách hàng", description = "Xoá mềm một khách hàng theo ID và chi nhánh")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xoá khách hàng thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy khách hàng")
    })
    public ApiResponseDto<?> delete(
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestParam @Parameter(description = "ID của chi nhánh") String branchId,
            @PathVariable @Parameter(description = "ID của khách hàng") String id) {
        customerService.deleteCustomer(shopId, branchId, id);
        return ApiResponseDto.success(ApiCode.CUSTOMER_DELETED);
    }
}
