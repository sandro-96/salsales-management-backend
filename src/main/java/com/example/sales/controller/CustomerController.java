package com.example.sales.controller;

import com.example.sales.constant.ApiMessage;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.customer.CustomerRequest;
import com.example.sales.dto.customer.CustomerResponse;
import com.example.sales.model.User;
import com.example.sales.service.CustomerService;
import com.example.sales.util.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final MessageService messageService;

    @GetMapping
    public ApiResponse<List<CustomerResponse>> getAll(@AuthenticationPrincipal User user,
                                                        @RequestParam(required = false) String branchId,
                                                      Locale locale) {
        return ApiResponse.success(ApiMessage.CUSTOMER_LIST, customerService.getCustomers(user, branchId), messageService, locale);
    }

    @PostMapping
    public ApiResponse<CustomerResponse> create(@AuthenticationPrincipal User user,
                                                @RequestBody @Valid CustomerRequest request,
                                                Locale locale) {
        return ApiResponse.success(ApiMessage.CUSTOMER_CREATED, customerService.createCustomer(user, request), messageService, locale);
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerResponse> update(@AuthenticationPrincipal User user,
                                                @PathVariable String id,
                                                @RequestBody @Valid CustomerRequest request,
                                                Locale locale) {
        return ApiResponse.success(ApiMessage.CUSTOMER_UPDATED, customerService.updateCustomer(user, id, request), messageService, locale);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@AuthenticationPrincipal User user,
                                 @PathVariable String id,
                                 @RequestParam(required = false) String branchId,
                                 Locale locale) {
        customerService.deleteCustomer(user, branchId, id);
        return ApiResponse.success(ApiMessage.CUSTOMER_DELETED, messageService, locale);
    }
}
