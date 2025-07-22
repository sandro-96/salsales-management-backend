// File: src/main/java/com/example/sales/controller/TableController.java
package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.Permission;
import com.example.sales.constant.TableStatus;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.table.TableRequest;
import com.example.sales.dto.table.TableResponse;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequirePermission;
import com.example.sales.service.TableService;
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
@RequestMapping("/api/tables")
@RequiredArgsConstructor
@Validated
public class TableController {

    private final TableService tableService;

    @PostMapping
    @Operation(summary = "Tạo bàn mới", description = "Tạo một bàn mới trong cửa hàng với thông tin bàn và chi nhánh")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bàn được tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ hoặc tên bàn đã tồn tại"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Cửa hàng không tìm thấy")
    })
    public ApiResponseDto<TableResponse> create(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestBody @Valid @Parameter(description = "Thông tin bàn") TableRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS, tableService.create(user.getId(), request));
    }

    @GetMapping
    @RequirePermission(Permission.TABLE_CREATE)
    @Operation(summary = "Lấy danh sách bàn", description = "Lấy danh sách bàn của cửa hàng với phân trang")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Danh sách bàn được trả về thành công"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Cửa hàng không tìm thấy")
    })
    public ApiResponseDto<Page<TableResponse>> getByShop(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestParam @Parameter(description = "ID của chi nhánh (tùy chọn)") String branchId,
            @Parameter(description = "Thông tin phân trang (page, size, sort)") Pageable pageable) {
        return ApiResponseDto.success(ApiCode.SUCCESS, tableService.getByShop(user.getId(), shopId, branchId, pageable));
    }

    @PutMapping("/{id}/status")
    @RequirePermission(Permission.TABLE_UPDATE)
    @Operation(summary = "Cập nhật trạng thái bàn", description = "Cập nhật trạng thái bàn (AVAILABLE, OCCUPIED, v.v.)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trạng thái bàn được cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Trạng thái không hợp lệ hoặc bàn không thể cập nhật"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Bàn hoặc cửa hàng không tìm thấy")
    })
    public ApiResponseDto<TableResponse> updateStatus(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @PathVariable @Parameter(description = "ID của bàn") String id,
            @RequestParam @Parameter(description = "Trạng thái mới của bàn") TableStatus status) {
        return ApiResponseDto.success(ApiCode.SUCCESS, tableService.updateStatus(user.getId(), id, status));
    }

    @PutMapping("/{id}")
    @RequirePermission(Permission.TABLE_UPDATE)
    @Operation(summary = "Cập nhật thông tin bàn", description = "Cập nhật thông tin bàn như tên, sức chứa, ghi chú")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thông tin bàn được cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ hoặc bàn đang được sử dụng"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Bàn hoặc cửa hàng không tìm thấy")
    })
    public ApiResponseDto<TableResponse> updateTable(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @PathVariable @Parameter(description = "ID của bàn") String id,
            @RequestBody @Valid @Parameter(description = "Thông tin cập nhật bàn") TableRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS, tableService.updateTable(user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Permission.TABLE_DELETE)
    @Operation(summary = "Xóa bàn", description = "Xóa mềm một bàn nếu bàn không đang được sử dụng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bàn được xóa thành công"),
            @ApiResponse(responseCode = "400", description = "Bàn đang được sử dụng"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Bàn hoặc cửa hàng không tìm thấy")
    })
    public ApiResponseDto<?> deleteTable(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @PathVariable @Parameter(description = "ID của bàn") String id) {
        tableService.deleteTable(user.getId(), id);
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }
}
