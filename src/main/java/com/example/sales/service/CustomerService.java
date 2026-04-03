package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.customer.CustomerRequest;
import com.example.sales.dto.customer.CustomerResponse;
import com.example.sales.dto.customer.CustomerSearchRequest;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.helper.CustomerSearchHelper;
import com.example.sales.model.Customer;
import com.example.sales.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerSearchHelper customerSearchHelper;
    private final AuditLogService auditLogService;
    private final ExcelExportService excelExportService;

    public Page<CustomerResponse> searchCustomers(String shopId, String branchId, CustomerSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        List<Customer> customers = customerSearchHelper.search(shopId, branchId, request, pageable);
        long total = customerSearchHelper.count(shopId, branchId, request);
        List<CustomerResponse> responses = customers.stream().map(this::toResponse).toList();
        return new PageImpl<>(responses, pageable, total);
    }

    public CustomerResponse getById(String shopId, String id) {
        Customer customer = customerRepository.findByIdAndDeletedFalse(id)
                .filter(c -> c.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.CUSTOMER_NOT_FOUND));
        return toResponse(customer);
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

    public CustomerResponse updateCustomer(String shopId, String userId, String id, CustomerRequest request) {
        Customer existing = customerRepository.findByIdAndDeletedFalse(id)
                .filter(c -> c.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.CUSTOMER_NOT_FOUND));

        if (existing.getBranchId() != null && !existing.getBranchId().equals(request.getBranchId())) {
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }

        existing.setName(request.getName());
        existing.setPhone(request.getPhone());
        existing.setEmail(request.getEmail());
        existing.setAddress(request.getAddress());
        existing.setNote(request.getNote());

        Customer saved = customerRepository.save(existing);
        auditLogService.log(userId, shopId, saved.getId(), "CUSTOMER", "UPDATED",
                String.format("Cập nhật khách hàng: %s (%s)", saved.getName(), saved.getPhone()));
        return toResponse(saved);
    }

    public void deleteCustomer(String shopId, String userId, String branchId, String id) {
        Customer customer = customerRepository.findByIdAndDeletedFalse(id)
                .filter(c -> c.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.CUSTOMER_NOT_FOUND));

        if (customer.getBranchId() != null && !customer.getBranchId().equals(branchId)) {
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }

        customer.setDeleted(true);
        customerRepository.save(customer);
        auditLogService.log(userId, shopId, customer.getId(), "CUSTOMER", "DELETED",
                String.format("Xoá mềm khách hàng: %s (%s)", customer.getName(), customer.getPhone()));
    }

    public ResponseEntity<byte[]> exportCustomers(String shopId, String branchId, CustomerSearchRequest request) {
        List<Customer> customers = customerSearchHelper.exportAll(shopId, branchId, request);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        return excelExportService.exportExcel(
                "customers.xlsx",
                "Khách hàng",
                List.of("Tên", "Số điện thoại", "Email", "Địa chỉ", "Ghi chú", "Ngày tạo"),
                customers,
                c -> List.of(
                        safe(c.getName()),
                        safe(c.getPhone()),
                        safe(c.getEmail()),
                        safe(c.getAddress()),
                        safe(c.getNote()),
                        c.getCreatedAt() != null ? c.getCreatedAt().format(df) : ""
                )
        );
    }

    private CustomerResponse toResponse(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .phone(c.getPhone())
                .email(c.getEmail())
                .address(c.getAddress())
                .note(c.getNote())
                .branchId(c.getBranchId())
                .loyaltyPoints(c.getLoyaltyPoints())
                .totalPointsEarned(c.getTotalPointsEarned())
                .totalPointsRedeemed(c.getTotalPointsRedeemed())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}
