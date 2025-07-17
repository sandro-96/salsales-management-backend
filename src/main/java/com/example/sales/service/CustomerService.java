// File: src/main/java/com/example/sales/service/CustomerService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.customer.CustomerRequest;
import com.example.sales.dto.customer.CustomerResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Customer;
import com.example.sales.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;

    public List<CustomerResponse> getCustomers(String shopId, String branchId) {
        return customerRepository.findByShopIdAndBranchIdAndDeletedFalse(shopId, branchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CustomerResponse createCustomer(String shopId, String userId, CustomerRequest request) {
        Customer customer = new Customer();
        customer.setShopId(shopId);
        customer.setUserId(userId);
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setNote(request.getNote());
        customer.setBranchId(request.getBranchId());

        Customer saved = customerRepository.save(customer);
        auditLogService.log(userId, shopId, saved.getId(), "CUSTOMER", "CREATED",
                String.format("Tạo khách hàng: %s (%s)", saved.getName(), saved.getPhone()));
        return toResponse(saved);
    }

    public CustomerResponse updateCustomer(String shopId, String id, CustomerRequest request) {
        Customer existing = customerRepository.findByIdAndDeletedFalse(id)
                .filter(c -> c.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.CUSTOMER_NOT_FOUND));

        if (!existing.getBranchId().equals(request.getBranchId())) {
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }

        existing.setName(request.getName());
        existing.setPhone(request.getPhone());
        existing.setEmail(request.getEmail());
        existing.setAddress(request.getAddress());
        existing.setNote(request.getNote());

        Customer saved = customerRepository.save(existing);
        auditLogService.log(null, shopId, saved.getId(), "CUSTOMER", "UPDATED",
                String.format("Cập nhật khách hàng: %s (%s)", saved.getName(), saved.getPhone()));
        return toResponse(saved);
    }

    public void deleteCustomer(String shopId, String branchId, String id) {
        Customer customer = customerRepository.findByIdAndDeletedFalse(id)
                .filter(c -> c.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.CUSTOMER_NOT_FOUND));

        if (!customer.getBranchId().equals(branchId)) {
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }

        customer.setDeleted(true);
        customerRepository.save(customer);
        auditLogService.log(null, shopId, customer.getId(), "CUSTOMER", "DELETED",
                String.format("Xoá mềm khách hàng: %s (%s)", customer.getName(), customer.getPhone()));
    }

    private CustomerResponse toResponse(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .phone(c.getPhone())
                .email(c.getEmail())
                .address(c.getAddress())
                .note(c.getNote())
                .build();
    }
}
