package com.example.sales.service;

import com.example.sales.constant.ApiErrorCode;
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
    public List<Customer> getCustomers(User user) {
        Shop shop = getShopOfUser(user);
        return customerRepository.findByShopId(shop.getId());
    }

    // Tạo khách hàng mới, gắn shopId
    public Customer createCustomer(User user, Customer customer) {
        Shop shop = getShopOfUser(user);

        customer.setId(null);
        customer.setShopId(shop.getId());

        return customerRepository.save(customer);
    }

    // Cập nhật khách hàng
    public Customer updateCustomer(User user, String id, Customer updated) {
        Shop shop = getShopOfUser(user);

        Customer existing = customerRepository.findById(id)
                .filter(c -> c.getShopId().equals(shop.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.CUSTOMER_NOT_FOUND));

        existing.setName(updated.getName());
        existing.setPhone(updated.getPhone());
        existing.setEmail(updated.getEmail());
        existing.setAddress(updated.getAddress());

        return customerRepository.save(existing);
    }

    // Xoá khách hàng
    public void deleteCustomer(User user, String id) {
        Shop shop = getShopOfUser(user);

        Customer customer = customerRepository.findById(id)
                .filter(c -> c.getShopId().equals(shop.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.CUSTOMER_NOT_FOUND));

        customerRepository.delete(customer);
    }

    // Helper: lấy shop của user hiện tại
    private Shop getShopOfUser(User user) {
        return shopRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new BusinessException(ApiErrorCode.SHOP_NOT_FOUND));
    }
}
