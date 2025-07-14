// File: src/main/java/com/example/sales/controller/CustomerController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.customer.CustomerRequest;
import com.example.sales.dto.customer.CustomerResponse;
import com.example.sales.model.User;
import com.example.sales.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Validated
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ApiResponse<List<CustomerResponse>> getAll(@AuthenticationPrincipal User user,
                                                      @RequestParam(required = false) String branchId) {
        return ApiResponse.success(ApiCode.SUCCESS, customerService.getCustomers(user, branchId));
    }

    @PostMapping
    public ApiResponse<CustomerResponse> create(@AuthenticationPrincipal User user,
                                                @RequestBody @Valid CustomerRequest request) {
        return ApiResponse.success(ApiCode.SUCCESS, customerService.createCustomer(user, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerResponse> update(@AuthenticationPrincipal User user,
                                                @PathVariable String id,
                                                @RequestBody @Valid CustomerRequest request) {
        return ApiResponse.success(ApiCode.SUCCESS, customerService.updateCustomer(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@AuthenticationPrincipal User user,
                                 @PathVariable String id,
                                 @RequestParam(required = false) String branchId) {
        customerService.deleteCustomer(user, branchId, id);
        return ApiResponse.success(ApiCode.SUCCESS);
    }
}
