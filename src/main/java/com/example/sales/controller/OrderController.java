// File: src/main/java/com/example/sales/controller/OrderController.java
package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.OrderStatus;
import com.example.sales.constant.Permission;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.order.OrderFulfillmentPatchRequest;
import com.example.sales.dto.order.OrderRequest;
import com.example.sales.dto.order.OrderResponse;
import com.example.sales.dto.order.OrderUpdateRequest;
import com.example.sales.model.tax.OrderTaxSnapshot;
import com.example.sales.service.tax.OrderTaxApplier;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequirePermission;
import com.example.sales.service.OrderService;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;
    private final OrderTaxApplier orderTaxApplier;

    @GetMapping
    @RequirePermission(Permission.ORDER_VIEW)
    @Operation(summary = "Lấy danh sách đơn hàng", description = "Lấy danh sách đơn hàng của cửa hàng với phân trang")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Danh sách đơn hàng được trả về thành công"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Cửa hàng không tìm thấy")
    })
    public ApiResponseDto<Page<OrderResponse>> getShopOrders(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestParam(required = false) @Parameter(description = "Lọc theo chi nhánh; bỏ qua = tất cả chi nhánh") String branchId,
            @Parameter(description = "Thông tin phân trang (page, size, sort)") Pageable pageable) {
        Page<OrderResponse> orders = orderService.getShopOrders(shopId, branchId, pageable);
        return ApiResponseDto.success(ApiCode.ORDER_LIST, orders);
    }

    @GetMapping("/preview-tax")
    @RequirePermission(Permission.ORDER_CREATE)
    @Operation(summary = "Xem trước thuế đơn hàng", description = "Tính thuế theo chính sách hiện hành từ tổng tiền hàng (chưa cộng thuế hoặc đã gồm thuế tùy cấu hình cửa hàng)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tính thuế thành công"),
            @ApiResponse(responseCode = "400", description = "totalPrice không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này")
    })
    public ApiResponseDto<OrderTaxSnapshot> previewTax(
            @RequestParam @Parameter(description = "ID cửa hàng") String shopId,
            @RequestParam @Parameter(description = "ID chi nhánh") String branchId,
            @RequestParam @Parameter(description = "Tổng tiền hàng (cùng nghĩa totalPrice lúc tạo đơn)") double totalPrice) {
        if (totalPrice < 0 || totalPrice > 1_000_000_000_000d) {
            return ApiResponseDto.error(ApiCode.VALIDATION_ERROR, "totalPrice must be between 0 and 1e12", null);
        }
        OrderTaxSnapshot snapshot = orderTaxApplier.preview(shopId, branchId, totalPrice);
        return ApiResponseDto.success(ApiCode.ORDER_TAX_PREVIEW, snapshot);
    }

    @PostMapping
    @RequirePermission(Permission.ORDER_CREATE)
    @Operation(summary = "Tạo đơn hàng mới", description = "Tạo đơn hàng với danh sách sản phẩm, thông tin bàn và chi nhánh")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đơn hàng được tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Cửa hàng, sản phẩm hoặc bàn không tìm thấy")
    })
    public ApiResponseDto<OrderResponse> createOrder(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestParam @Parameter(description = "ID của chi nhánh (nếu có)") String branchId,
            @RequestBody @Valid @Parameter(description = "Thông tin đơn hàng") OrderRequest request) {
        OrderResponse created = orderService.createOrder(user.getId(), branchId, shopId, request);
        return ApiResponseDto.success(ApiCode.ORDER_CREATED, created);
    }

    @PutMapping("/{id}/cancel")
    @RequirePermission(Permission.ORDER_CANCEL)
    @Operation(summary = "Hủy đơn hàng", description = "Hủy đơn hàng nếu chưa được thanh toán")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đơn hàng được hủy thành công"),
            @ApiResponse(responseCode = "400", description = "Đơn hàng đã được thanh toán"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Đơn hàng hoặc cửa hàng không tìm thấy")
    })
    public ApiResponseDto<?> cancelOrder(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @PathVariable @Parameter(description = "ID của đơn hàng") String id) {
        orderService.cancelOrder(user.getId(), shopId, id);
        return ApiResponseDto.success(ApiCode.ORDER_CANCELLED);
    }

    @PostMapping("/{orderId}/confirm-payment")
    @RequirePermission(Permission.ORDER_PAYMENT_CONFIRM)
    @Operation(summary = "Xác nhận thanh toán", description = "Xác nhận thanh toán cho đơn hàng và cập nhật trạng thái")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thanh toán được xác nhận thành công"),
            @ApiResponse(responseCode = "400", description = "Đơn hàng đã được thanh toán"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Đơn hàng hoặc cửa hàng không tìm thấy")
    })
    public ApiResponseDto<OrderResponse> confirmPayment(
            @PathVariable @Parameter(description = "ID của đơn hàng") String orderId,
            @RequestParam @Parameter(description = "ID của giao dịch thanh toán") String paymentId,
            @RequestParam @Parameter(description = "Phương thức thanh toán") String paymentMethod,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user) {
        OrderResponse confirmed = orderService.confirmPayment(user.getId(), shopId, orderId, paymentId, paymentMethod);
        return ApiResponseDto.success(ApiCode.ORDER_PAYMENT_CONFIRMED, confirmed);
    }

    @PutMapping("/{id}")
    @RequirePermission(Permission.ORDER_UPDATE)
    @Operation(summary = "Cập nhật đơn hàng", description = "Cập nhật thông tin đơn hàng như bàn, ghi chú hoặc danh sách sản phẩm (chỉ khi đơn chưa thanh toán)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đơn hàng được cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Đơn hàng đã thanh toán hoặc dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Đơn hàng hoặc cửa hàng không tìm thấy")
    })
    public ApiResponseDto<OrderResponse> updateOrder(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @PathVariable("id") @Parameter(description = "ID đơn hàng") String orderId,
            @RequestParam @Parameter(description = "ID cửa hàng") String shopId,
            @Valid @RequestBody @Parameter(description = "Thông tin cập nhật đơn hàng") OrderUpdateRequest request) {

        OrderResponse response = orderService.updateOrder(user.getId(), shopId, orderId, request);
        return ApiResponseDto.success(ApiCode.ORDER_UPDATED, response);
    }

    @PatchMapping("/{id}/fulfillment")
    @RequirePermission(Permission.ORDER_UPDATE)
    @Operation(summary = "Cập nhật giao hàng / tham chiếu đơn", description = "Ghi chú, khách hàng, vận đơn, đơn vị VC — cho phép cả đơn đã thanh toán (trừ đơn đã hủy)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Đơn đã hủy hoặc dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Đơn hoặc khách không tìm thấy")
    })
    public ApiResponseDto<OrderResponse> patchOrderFulfillment(
            @AuthenticationPrincipal @Parameter(hidden = true) CustomUserDetails user,
            @PathVariable("id") @Parameter(description = "ID đơn hàng") String orderId,
            @RequestParam @Parameter(description = "ID cửa hàng") String shopId,
            @Valid @RequestBody OrderFulfillmentPatchRequest request) {
        OrderResponse response = orderService.patchOrderFulfillment(user.getId(), shopId, orderId, request);
        return ApiResponseDto.success(ApiCode.ORDER_UPDATED, response);
    }

    @PutMapping("/{id}/status")
    @RequirePermission(Permission.ORDER_UPDATE)
    @Operation(summary = "Cập nhật trạng thái đơn hàng", description = "Cập nhật trạng thái đơn hàng (PENDING, COMPLETED, CANCELLED, v.v.)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trạng thái đơn hàng được cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Đơn hàng đã bị hủy hoặc trạng thái không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Đơn hàng hoặc cửa hàng không tìm thấy")
    })
    public ApiResponseDto<OrderResponse> updateStatus(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @PathVariable @Parameter(description = "ID của đơn hàng") String id,
            @RequestParam @Parameter(description = "Trạng thái mới của đơn hàng") OrderStatus status) {
        OrderResponse updated = orderService.updateStatus(user.getId(), shopId, id, status);
        return ApiResponseDto.success(ApiCode.ORDER_STATUS_UPDATED, updated);
    }

    @GetMapping("/filter")
    @RequirePermission(Permission.ORDER_VIEW)
    @Operation(summary = "Lấy đơn hàng theo trạng thái", description = "Lấy danh sách đơn hàng theo trạng thái và chi nhánh với phân trang")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Danh sách đơn hàng được trả về thành công"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thực hiện hành động này"),
            @ApiResponse(responseCode = "404", description = "Cửa hàng không tìm thấy")
    })
    public ApiResponseDto<Page<OrderResponse>> getByStatus(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestParam @Parameter(description = "ID của cửa hàng") String shopId,
            @RequestParam @Parameter(description = "Trạng thái đơn hàng (PENDING, COMPLETED, CANCELLED, v.v.)") OrderStatus status,
            @RequestParam(required = false) @Parameter(description = "ID của chi nhánh (tùy chọn)") String branchId,
            @Parameter(description = "Thông tin phân trang (page, size, sort)") Pageable pageable) {
        Page<OrderResponse> filtered = orderService.getOrdersByStatus(shopId, status, branchId, pageable);
        return ApiResponseDto.success(ApiCode.ORDER_LIST, filtered);
    }
}
