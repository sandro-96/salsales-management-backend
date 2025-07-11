// File: src/main/java/com/example/sales/controller/OrderController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiMessage;
import com.example.sales.constant.OrderStatus;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.order.OrderRequest;
import com.example.sales.dto.order.OrderResponse;
import com.example.sales.model.User;
import com.example.sales.service.OrderService;
import com.example.sales.util.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final MessageService messageService;

    // ✅ 1. Lấy danh sách đơn hàng của user (shop owner)
    @GetMapping
    public ApiResponse<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal User user, Locale locale) {
        List<OrderResponse> orders = orderService.getOrdersByUser(user);
        return ApiResponse.success(ApiMessage.ORDER_LIST, orders, messageService, locale);
    }

    // ✅ 2. Tạo đơn hàng mới
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(@AuthenticationPrincipal User user,
                                                  @RequestBody @Valid OrderRequest request,
                                                  Locale locale) {
        OrderResponse created = orderService.createOrder(user, request);
        return ApiResponse.success(ApiMessage.ORDER_CREATED, created, messageService, locale);
    }

    // ✅ 3. Hủy đơn
    @PutMapping("/{id}/cancel")
    public ApiResponse<?> cancelOrder(@AuthenticationPrincipal User user,
                                      @PathVariable String id,
                                      Locale locale) {
        orderService.cancelOrder(user, id);
        return ApiResponse.success(ApiMessage.ORDER_CANCELLED, messageService, locale);
    }

    // ✅ 4. Xác nhận thanh toán
    @PostMapping("/{orderId}/confirm-payment")
    public ApiResponse<OrderResponse> confirmPayment(@PathVariable String orderId,
                                                     @RequestParam String paymentId,
                                                     @RequestParam String paymentMethod,
                                                     @AuthenticationPrincipal User user,
                                                     Locale locale) {
        OrderResponse confirmed = orderService.confirmPayment(user, orderId, paymentId, paymentMethod);
        return ApiResponse.success(ApiMessage.ORDER_PAYMENT_CONFIRMED, confirmed, messageService, locale);
    }

    // ✅ 5. Cập nhật trạng thái đơn hàng
    @PutMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(@AuthenticationPrincipal User user,
                                                   @PathVariable String id,
                                                   @RequestParam OrderStatus status,
                                                   Locale locale) {
        OrderResponse updated = orderService.updateStatus(user, id, status);
        return ApiResponse.success(ApiMessage.ORDER_STATUS_UPDATED, updated, messageService, locale);
    }

    // ✅ 6. Lọc đơn theo trạng thái
    @GetMapping("/filter")
    public ApiResponse<List<OrderResponse>> getByStatus(@AuthenticationPrincipal User user,
                                                        @RequestParam OrderStatus status,
                                                        @RequestParam(required = false) String branchId,
                                                        Locale locale) {
        List<OrderResponse> filtered = orderService.getOrdersByStatus(user, status, branchId);
        return ApiResponse.success(ApiMessage.ORDER_LIST, filtered, messageService, locale);
    }
}
