package com.example.sales.service;

import com.example.sales.constant.ApiErrorCode;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Product;
import com.example.sales.model.User;
import com.example.sales.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAllByUser(User user) {
        return productRepository.findByUserId(user.getId());
    }

    public Product createProduct(User user, Product product) {
        product.setId(null); // Đảm bảo là tạo mới
        product.setUserId(user.getId());
        return productRepository.save(product);
    }

    public Product updateProduct(User user, String id, Product updated) {
        Product existing = productRepository.findById(id)
                .filter(p -> p.getUserId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.PRODUCT_NOT_FOUND));

        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setQuantity(updated.getQuantity());
        existing.setPrice(updated.getPrice());

        return productRepository.save(existing);
    }

    public void deleteProduct(User user, String id) {
        Product existing = productRepository.findById(id)
                .filter(p -> p.getUserId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.PRODUCT_NOT_FOUND));

        productRepository.delete(existing);
    }
}
