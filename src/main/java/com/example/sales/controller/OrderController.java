// File: src/main/java/com/example/sales/controller/OrderController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.OrderStatus;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.order.OrderRequest;
import com.example.sales.dto.order.OrderResponse;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequireRole;
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
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal CustomUserDetails user,
                                                        @RequestParam String shopId) {
        List<OrderResponse> orders = orderService.getOrdersByUser(user.getId(), shopId);
        return ApiResponse.success(ApiCode.ORDER_LIST, orders);
    }

    @PostMapping
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<OrderResponse> createOrder(@AuthenticationPrincipal CustomUserDetails user,
                                                  @RequestParam String shopId,
                                                  @RequestBody @Valid OrderRequest request) {
        OrderResponse created = orderService.createOrder(user.getId(), shopId, request);
        return ApiResponse.success(ApiCode.ORDER_CREATED, created);
    }

    @PutMapping("/{id}/cancel")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<?> cancelOrder(@AuthenticationPrincipal CustomUserDetails user,
                                      @RequestParam String shopId,
                                      @PathVariable String id) {
        orderService.cancelOrder(user.getId(), shopId, id);
        return ApiResponse.success(ApiCode.ORDER_CANCELLED);
    }

    @PostMapping("/{orderId}/confirm-payment")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<OrderResponse> confirmPayment(@PathVariable String orderId,
                                                     @RequestParam String paymentId,
                                                     @RequestParam String paymentMethod,
                                                     @RequestParam String shopId,
                                                     @AuthenticationPrincipal CustomUserDetails user) {
        OrderResponse confirmed = orderService.confirmPayment(user.getId(), shopId, orderId, paymentId, paymentMethod);
        return ApiResponse.success(ApiCode.ORDER_PAYMENT_CONFIRMED, confirmed);
    }

    @PutMapping("/{id}/status")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<OrderResponse> updateStatus(@AuthenticationPrincipal CustomUserDetails user,
                                                   @RequestParam String shopId,
                                                   @PathVariable String id,
                                                   @RequestParam OrderStatus status) {
        OrderResponse updated = orderService.updateStatus(user.getId(), shopId, id, status);
        return ApiResponse.success(ApiCode.ORDER_STATUS_UPDATED, updated);
    }

    @GetMapping("/filter")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<List<OrderResponse>> getByStatus(@AuthenticationPrincipal CustomUserDetails user,
                                                        @RequestParam String shopId,
                                                        @RequestParam OrderStatus status,
                                                        @RequestParam(required = false) String branchId) {
        List<OrderResponse> filtered = orderService.getOrdersByStatus(user.getId(), shopId, status, branchId);
        return ApiResponse.success(ApiCode.ORDER_LIST, filtered);
    }
}
