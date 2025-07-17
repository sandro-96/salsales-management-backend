// File: src/main/java/com/example/sales/controller/CustomerController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.customer.CustomerRequest;
import com.example.sales.dto.customer.CustomerResponse;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequireRole;
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
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<List<CustomerResponse>> getAll(@RequestParam String shopId,
                                                      @RequestParam(required = false) String branchId) {
        return ApiResponse.success(ApiCode.CUSTOMER_LIST, customerService.getCustomers(shopId, branchId));
    }

    @PostMapping
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<CustomerResponse> create(@AuthenticationPrincipal CustomUserDetails user,
                                                @RequestParam String shopId,
                                                @RequestBody @Valid CustomerRequest request) {
        return ApiResponse.success(ApiCode.CUSTOMER_CREATED, customerService.createCustomer(shopId, user.getId(), request));
    }

    @PutMapping("/{id}")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<CustomerResponse> update(@RequestParam String shopId,
                                                @PathVariable String id,
                                                @RequestBody @Valid CustomerRequest request) {
        return ApiResponse.success(ApiCode.CUSTOMER_UPDATED, customerService.updateCustomer(shopId, id, request));
    }

    @DeleteMapping("/{id}")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<?> delete(@RequestParam String shopId,
                                 @RequestParam String branchId,
                                 @PathVariable String id) {
        customerService.deleteCustomer(shopId, branchId, id);
        return ApiResponse.success(ApiCode.CUSTOMER_DELETED);
    }
}
