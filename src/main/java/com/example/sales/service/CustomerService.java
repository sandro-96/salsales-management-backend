// File: src/main/java/com/example/sales/service/CustomerService.java
package com.example.sales.service;

import com.example.sales.constant.ApiErrorCode;
import com.example.sales.dto.customer.CustomerRequest;
import com.example.sales.dto.customer.CustomerResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Customer;
import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.repository.CustomerRepository;
import com.example.sales.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ShopRepository shopRepository;

    // Lấy danh sách khách hàng thuộc shop của user
    public List<CustomerResponse> getCustomers(User user, String branchId) {
        Shop shop = getShopOfUser(user);
        return customerRepository.findByShopIdAndBranchId(shop.getId(), branchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Tạo khách hàng mới, gắn shopId
    public CustomerResponse createCustomer(User user, CustomerRequest request) {
        Shop shop = getShopOfUser(user);

        Customer customer = new Customer();
        customer.setShopId(shop.getId());
        customer.setUserId(user.getId());
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setNote(request.getNote());
        customer.setBranchId(request.getBranchId());

        return toResponse(customerRepository.save(customer));
    }

    // Cập nhật khách hàng
    public CustomerResponse updateCustomer(User user, String id, CustomerRequest request) {
        Shop shop = getShopOfUser(user);

        Customer existing = customerRepository.findById(id)
                .filter(c -> c.getShopId().equals(shop.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.CUSTOMER_NOT_FOUND));
        if (!existing.getBranchId().equals(request.getBranchId())) {
            throw new BusinessException(ApiErrorCode.UNAUTHORIZED);
        }
        existing.setName(request.getName());
        existing.setPhone(request.getPhone());
        existing.setEmail(request.getEmail());
        existing.setAddress(request.getAddress());
        existing.setNote(request.getNote());

        return toResponse(customerRepository.save(existing));
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

    // Xoá khách hàng
    public void deleteCustomer(User user, String branchId, String id) {
        Shop shop = getShopOfUser(user);

        Customer customer = customerRepository.findById(id)
                .filter(c -> c.getShopId().equals(shop.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.CUSTOMER_NOT_FOUND));
        if (!customer.getBranchId().equals(branchId)) {
            throw new BusinessException(ApiErrorCode.UNAUTHORIZED);
        }
        customerRepository.delete(customer);
    }

    // Helper: lấy shop của user hiện tại
    private Shop getShopOfUser(User user) {
        return shopRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new BusinessException(ApiErrorCode.SHOP_NOT_FOUND));
    }
}
