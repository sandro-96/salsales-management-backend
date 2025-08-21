// File: src/main/java/com/example/sales/repository/ProductRepository.java
package com.example.sales.repository;

import com.example.sales.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findByIdAndDeletedFalse(String id);
    // Tìm Product theo ID và ShopId
    Optional<Product> findByIdAndShopIdAndDeletedFalse(String id, String shopId);

    // Tìm Product theo ShopId và SKU
    Optional<Product> findByShopIdAndSkuAndDeletedFalse(String shopId, String sku);

    // Tìm Product theo ShopId và Barcode
    Optional<Product> findByShopIdAndBarcodeAndDeletedFalse(String shopId, String barcode);

    // Kiểm tra sự tồn tại của Product theo ShopId và SKU
    boolean existsByShopIdAndSkuAndDeletedFalse(String shopId, String sku);

    // Tìm kiếm các sản phẩm chung theo tên hoặc danh mục (trong Product)
    // Lưu ý: Đây là tìm kiếm trên định nghĩa sản phẩm chung, không phải tồn kho cụ thể của chi nhánh
    Optional<Product> findByShopIdAndNameContainingIgnoreCaseAndDeletedFalse(String shopId, String name);
    Optional<Product> findByShopIdAndCategoryContainingIgnoreCaseAndDeletedFalse(String shopId, String category);


    // Chúng ta sẽ cần thêm các phương thức tìm kiếm linh hoạt hơn
    // Hoặc xây dựng query động trong ProductServiceImpl sử dụng MongoTemplate hoặc Aggregation
}