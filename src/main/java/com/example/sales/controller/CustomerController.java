package com.example.sales.controller;

import com.example.sales.constant.ApiMessage;
import com.example.sales.dto.ApiResponse;
import com.example.sales.model.Customer;
import com.example.sales.model.User;
import com.example.sales.service.CustomerService;
import com.example.sales.util.MessageService;
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
    public ApiResponse<List<Customer>> getAll(@AuthenticationPrincipal User user, Locale locale) {
        return ApiResponse.success(ApiMessage.CUSTOMER_LIST, customerService.getCustomers(user), messageService, locale);
    }

    @PostMapping
    public ApiResponse<Customer> create(@AuthenticationPrincipal User user,
                                        @RequestBody Customer customer,
                                        Locale locale) {
        return ApiResponse.success(ApiMessage.CUSTOMER_CREATED, customerService.createCustomer(user, customer), messageService, locale);
    }

    @PutMapping("/{id}")
    public ApiResponse<Customer> update(@AuthenticationPrincipal User user,
                                        @PathVariable String id,
                                        @RequestBody Customer customer,
                                        Locale locale) {
        return ApiResponse.success(ApiMessage.CUSTOMER_UPDATED, customerService.updateCustomer(user, id, customer), messageService, locale);
    }
}
