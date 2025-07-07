package com.example.sales.controller;

import com.example.sales.constant.ApiMessage;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.CustomerSearchRequest;
import com.example.sales.model.Customer;
import com.example.sales.model.User;
import com.example.sales.service.CustomerService;
import com.example.sales.util.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
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
        return ApiResponse.success(ApiMessage.CUSTOMER_LIST, customerService.getAllByUser(user), messageService, locale);
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

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@AuthenticationPrincipal User user,
                                 @PathVariable String id,
                                 Locale locale) {
        customerService.deleteCustomer(user, id);
        return ApiResponse.success(ApiMessage.CUSTOMER_DELETED, messageService, locale);
    }

    @PostMapping("/search")
    public ApiResponse<Page<Customer>> search(@AuthenticationPrincipal User user,
                                              @RequestBody CustomerSearchRequest request,
                                              Locale locale) {
        Page<Customer> result = customerService.searchWithAggregation(user, request);
        return ApiResponse.success(ApiMessage.CUSTOMER_LIST, result, messageService, locale);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteCustomer(@AuthenticationPrincipal User user,
                                         @PathVariable String id,
                                         Locale locale) {
        customerService.softDeleteCustomer(user, id);
        return ApiResponse.success(ApiMessage.USER_DELETED, messageService, locale);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCustomers(@AuthenticationPrincipal User user) {
        return customerService.exportCustomersExcel(user);
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportFilteredCustomers(
            @AuthenticationPrincipal User user,
            @RequestBody CustomerSearchRequest request
    ) {
        return customerService.exportCustomers(user, request);
    }

    @PostMapping("/search")
    public ApiResponse<Page<Customer>> searchCustomers(
            @AuthenticationPrincipal User user,
            @RequestBody CustomerSearchRequest request,
            Locale locale
    ) {
        Page<Customer> result = customerService.searchCustomersPaged(user, request);
        return ApiResponse.success(ApiMessage.CUSTOMER_LIST, result, messageService, locale);
    }

}
