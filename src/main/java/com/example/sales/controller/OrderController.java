// File: src/main/java/com/example/sales/controller/OrderController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.OrderStatus;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.order.OrderRequest;
import com.example.sales.dto.order.OrderResponse;
import com.example.sales.model.User;
import com.example.sales.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal User user) {
        List<OrderResponse> orders = orderService.getOrdersByUser(user);
        return ApiResponse.success(ApiCode.SUCCESS, orders);
    }

    @PostMapping
    public ApiResponse<OrderResponse> createOrder(@AuthenticationPrincipal User user,
                                                  @RequestBody @Valid OrderRequest request) {
        OrderResponse created = orderService.createOrder(user, request);
        return ApiResponse.success(ApiCode.SUCCESS, created);
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<?> cancelOrder(@AuthenticationPrincipal User user,
                                      @PathVariable String id) {
        orderService.cancelOrder(user, id);
        return ApiResponse.success(ApiCode.SUCCESS);
    }

    @PostMapping("/{orderId}/confirm-payment")
    public ApiResponse<OrderResponse> confirmPayment(@PathVariable String orderId,
                                                     @RequestParam String paymentId,
                                                     @RequestParam String paymentMethod,
                                                     @AuthenticationPrincipal User user) {
        OrderResponse confirmed = orderService.confirmPayment(user, orderId, paymentId, paymentMethod);
        return ApiResponse.success(ApiCode.SUCCESS, confirmed);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(@AuthenticationPrincipal User user,
                                                   @PathVariable String id,
                                                   @RequestParam OrderStatus status) {
        OrderResponse updated = orderService.updateStatus(user, id, status);
        return ApiResponse.success(ApiCode.SUCCESS, updated);
    }

    @GetMapping("/filter")
    public ApiResponse<List<OrderResponse>> getByStatus(@AuthenticationPrincipal User user,
                                                        @RequestParam OrderStatus status,
                                                        @RequestParam(required = false) String branchId) {
        List<OrderResponse> filtered = orderService.getOrdersByStatus(user, status, branchId);
        return ApiResponse.success(ApiCode.SUCCESS, filtered);
    }
}
