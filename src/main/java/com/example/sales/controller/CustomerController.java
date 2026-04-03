package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.Permission;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.customer.AdjustPointsRequest;
import com.example.sales.dto.customer.CustomerRequest;
import com.example.sales.dto.customer.CustomerResponse;
import com.example.sales.dto.customer.CustomerSearchRequest;
import com.example.sales.dto.customer.PointTransactionResponse;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequirePermission;
import com.example.sales.service.CustomerService;
import com.example.sales.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Validated
public class CustomerController {

    private final CustomerService customerService;
    private final LoyaltyService loyaltyService;

    @GetMapping
    @RequirePermission(Permission.CUSTOMER_VIEW)
    @Operation(summary = "Tìm kiếm khách hàng", description = "Tìm kiếm khách hàng với phân trang, lọc theo từ khoá, khoảng ngày tạo, và sắp xếp")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách khách hàng thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ApiResponseDto<Page<CustomerResponse>> search(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestParam(required = false) @Parameter(description = "ID của chi nhánh (tuỳ chọn)") String branchId,
            @ModelAttribute CustomerSearchRequest searchRequest) {
        return ApiResponseDto.success(ApiCode.CUSTOMER_LIST, customerService.searchCustomers(shopId, branchId, searchRequest));
    }

    @GetMapping("/{id}")
    @RequirePermission(Permission.CUSTOMER_VIEW)
    @Operation(summary = "Lấy chi tiết khách hàng", description = "Lấy thông tin chi tiết một khách hàng theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy thông tin khách hàng thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy khách hàng")
    })
    public ApiResponseDto<CustomerResponse> getById(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @PathVariable @Parameter(description = "ID của khách hàng") String id) {
        return ApiResponseDto.success(ApiCode.SUCCESS, customerService.getById(shopId, id));
    }

    @PostMapping
    @RequirePermission(Permission.CUSTOMER_UPDATE)
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
    @RequirePermission(Permission.CUSTOMER_UPDATE)
    @Operation(summary = "Cập nhật khách hàng", description = "Cập nhật thông tin khách hàng theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật khách hàng thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy khách hàng")
    })
    public ApiResponseDto<CustomerResponse> update(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @PathVariable @Parameter(description = "ID của khách hàng") String id,
            @RequestBody @Valid @Parameter(description = "Thông tin cập nhật khách hàng") CustomerRequest request) {
        return ApiResponseDto.success(ApiCode.CUSTOMER_UPDATED, customerService.updateCustomer(shopId, user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Permission.CUSTOMER_DELETE)
    @Operation(summary = "Xóa khách hàng", description = "Xoá mềm một khách hàng theo ID và chi nhánh")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xoá khách hàng thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy khách hàng")
    })
    public ApiResponseDto<?> delete(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestParam @Parameter(description = "ID của chi nhánh") String branchId,
            @PathVariable @Parameter(description = "ID của khách hàng") String id) {
        customerService.deleteCustomer(shopId, user.getId(), branchId, id);
        return ApiResponseDto.success(ApiCode.CUSTOMER_DELETED);
    }

    @GetMapping("/export")
    @RequirePermission(Permission.CUSTOMER_VIEW)
    @Operation(summary = "Xuất danh sách khách hàng ra Excel", description = "Xuất danh sách khách hàng ra file Excel với hỗ trợ lọc theo từ khoá và khoảng ngày")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File Excel được xuất thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ResponseEntity<byte[]> export(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestParam(required = false) @Parameter(description = "ID của chi nhánh (tuỳ chọn)") String branchId,
            @ModelAttribute CustomerSearchRequest searchRequest) {
        return customerService.exportCustomers(shopId, branchId, searchRequest);
    }

    // ==================== LOYALTY POINTS ====================

    @GetMapping("/{id}/points")
    @RequirePermission(Permission.CUSTOMER_VIEW)
    @Operation(summary = "Lấy số dư điểm", description = "Lấy số điểm tích lũy hiện tại của khách hàng")
    public ApiResponseDto<Long> getPointsBalance(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam String shopId,
            @PathVariable String id) {
        return ApiResponseDto.success(ApiCode.SUCCESS, loyaltyService.getBalance(shopId, id));
    }

    @GetMapping("/{id}/points/history")
    @RequirePermission(Permission.CUSTOMER_VIEW)
    @Operation(summary = "Lịch sử điểm", description = "Lấy lịch sử giao dịch điểm tích lũy của khách hàng")
    public ApiResponseDto<Page<PointTransactionResponse>> getPointHistory(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam String shopId,
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponseDto.success(ApiCode.SUCCESS, loyaltyService.getPointHistory(shopId, id, page, size));
    }

    @PostMapping("/{id}/points/adjust")
    @RequirePermission(Permission.CUSTOMER_UPDATE)
    @Operation(summary = "Điều chỉnh điểm", description = "Cộng hoặc trừ điểm thủ công cho khách hàng")
    public ApiResponseDto<?> adjustPoints(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam String shopId,
            @PathVariable String id,
            @RequestBody @Valid AdjustPointsRequest request) {
        loyaltyService.adjustPoints(shopId, id, request.getPoints(), request.getNote(), user.getId());
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }
}
