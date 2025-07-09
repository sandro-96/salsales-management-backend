package com.example.sales.service;

import com.example.sales.constant.ApiErrorCode;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Product;
import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.repository.ProductRepository;
import com.example.sales.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;

    // Lấy sản phẩm thuộc shop của user
    public List<Product> getAllByUser(User user) {
        Shop shop = getShopOfUser(user);
        return productRepository.findByShopId(shop.getId());
    }

    // Tạo sản phẩm mới, gán shopId
    public Product createProduct(User user, Product product) {
        Shop shop = getShopOfUser(user);

        product.setId(null);
        product.setUserId(user.getId());
        product.setShopId(shop.getId());

        return productRepository.save(product);
    }

    // Cập nhật sản phẩm — phải cùng shop
    public Product updateProduct(User user, String id, Product updated) {
        Shop shop = getShopOfUser(user);

        Product existing = productRepository.findById(id)
                .filter(p -> p.getShopId().equals(shop.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.PRODUCT_NOT_FOUND));

        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setQuantity(updated.getQuantity());
        existing.setPrice(updated.getPrice());

        return productRepository.save(existing);
    }

    // Xóa sản phẩm — phải thuộc shop
    public void deleteProduct(User user, String id) {
        Shop shop = getShopOfUser(user);

        Product existing = productRepository.findById(id)
                .filter(p -> p.getShopId().equals(shop.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.PRODUCT_NOT_FOUND));

        productRepository.delete(existing);
    }

    // Helper: lấy shop của user hoặc lỗi nếu không có
    private Shop getShopOfUser(User user) {
        return shopRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new BusinessException(ApiErrorCode.SHOP_NOT_FOUND));
    }
}
