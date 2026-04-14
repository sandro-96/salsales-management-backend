// File: src/main/java/com/example/sales/service/ProductService.java
package com.example.sales.service;

import com.example.sales.dto.product.BranchProductRequest;
import com.example.sales.dto.product.ProductRequest;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.dto.product.ProductSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(String shopId, ProductRequest request);

    // id ở đây là id của Product
    ProductResponse updateProduct(String userId, String shopId, String id, ProductRequest request, List<MultipartFile> files);

    // Cập nhật thông tin BranchProduct (giá, tồn kho, discount...) tại một chi nhánh cụ thể
    ProductResponse updateBranchProduct(String userId, String shopId, String branchId, String branchProductId, BranchProductRequest request);

    ProductResponse createBranchProduct(String shopId, String branchId, ProductRequest request);


    // productId là id của Product — xóa sản phẩm khỏi toàn bộ shop (Product + tất cả BranchProduct)
    void deleteProductFromShop(String userId, String shopId, String productId);

    // id ở đây là id của BranchProduct
    ProductResponse getProduct(String shopId, String branchId, String id);

    // Toggle activeInBranch của BranchProduct tại một chi nhánh cụ thể
    ProductResponse toggleActiveInBranch(String userId, String shopId, String branchId, String branchProductId);

    // Toggle Product.active ở cấp shop — khi false sẽ sync tắt activeInBranch toàn bộ chi nhánh, khi true thì chỉ bật lại Product.active
    ProductResponse toggleActiveShop(String userId, String shopId, String productId);

    // Toggle Product.trackInventory ở cấp shop
    ProductResponse toggleTrackInventory(String userId, String shopId, String productId);

    // Tìm kiếm sản phẩm theo keyword, category, price range, active...
    Page<ProductResponse> searchProducts(String shopId, String branchId, ProductSearchRequest request, Pageable pageable);

    List<ProductResponse> getLowStockProducts(String shopId, String branchId, int threshold);

    String getSuggestedSku(String shopId, String industry, String category);

    String getSuggestedBarcode(String shopId, String industry, String category);

    // Upload ảnh cho sản phẩm — trả về danh sách URL ảnh sau khi thêm
    List<String> uploadProductImages(String userId, String shopId, String productId, List<MultipartFile> files);

    // Xóa một ảnh khỏi sản phẩm theo URL — trả về danh sách URL ảnh còn lại
    List<String> deleteProductImage(String userId, String shopId, String productId, String imageUrl);

    /**
     * Upload ảnh biến thể trước khi lưu Product (chưa cần productId).
     * Trả về URL để gắn vào {@link com.example.sales.model.ProductVariant#setImages}.
     */
    List<String> uploadStagedVariantImages(String userId, String shopId, List<MultipartFile> files);

    /**
     * Seed BranchProduct cho toàn bộ Products hiện có của shop khi tạo branch mới.
     * Mỗi Product sẽ có 1 BranchProduct với giá mặc định từ Product, tồn kho = 0.
     */
    void seedBranchProductsForNewBranch(String shopId, String branchId);
}

