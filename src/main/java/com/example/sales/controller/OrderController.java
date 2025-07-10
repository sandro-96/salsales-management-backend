package com.example.sales.controller;

import com.example.sales.constant.ApiMessage;
import com.example.sales.constant.OrderStatus;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.OrderRequest;
import com.example.sales.model.Order;
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

    @GetMapping
    public ApiResponse<List<Order>> getMyOrders(@AuthenticationPrincipal User user, Locale locale) {
        return ApiResponse.success(ApiMessage.ORDER_LIST, orderService.getOrdersByUser(user), messageService, locale);
    }

    @PostMapping
    public ApiResponse<Order> createOrder(@AuthenticationPrincipal User user,
                                          @RequestBody @Valid OrderRequest request,
                                          Locale locale) {
        Order created = orderService.createOrder(user, request);
        return ApiResponse.success(ApiMessage.ORDER_CREATED, created, messageService, locale);
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<?> cancelOrder(@AuthenticationPrincipal User user,
                                      @PathVariable String id,
                                      Locale locale) {
        orderService.cancelOrder(user, id);
        return ApiResponse.success(ApiMessage.ORDER_CANCELLED, messageService, locale);
    }

    @PostMapping("/{orderId}/confirm-payment")
    public ApiResponse<?> confirmPayment(@PathVariable String orderId,
                                         @RequestParam String paymentId,
                                         @RequestParam String paymentMethod,
                                         @AuthenticationPrincipal User user) {
        Order confirmed = orderService.confirmPayment(user, orderId, paymentId, paymentMethod);
        return ApiResponse.success(ApiMessage.ORDER_PAYMENT_CONFIRMED, confirmed, messageService, Locale.getDefault());
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Order> updateStatus(@AuthenticationPrincipal User user,
                                           @PathVariable String id,
                                           @RequestParam OrderStatus status,
                                           Locale locale) {
        Order updated = orderService.updateStatus(user, id, status);
        return ApiResponse.success(ApiMessage.ORDER_STATUS_UPDATED, updated, messageService, locale);
    }

    @GetMapping("/filter")
    public ApiResponse<List<Order>> getByStatus(@AuthenticationPrincipal User user,
                                                @RequestParam OrderStatus status,
                                                Locale locale) {
        return ApiResponse.success(ApiMessage.ORDER_LIST, orderService.getOrdersByStatus(user, status), messageService, locale);
    }

}

