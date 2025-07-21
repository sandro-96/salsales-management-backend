// File: src/main/java/com/example/sales/repository/BranchProductRepository.java
package com.example.sales.repository;

import com.example.sales.model.BranchProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface BranchProductRepository extends MongoRepository<BranchProduct, String> {

    // Tìm BranchProduct theo productId và branchId (đảm bảo duy nhất)
    Optional<BranchProduct> findByProductIdAndBranchIdAndDeletedFalse(String productId, String branchId);

    // Tìm BranchProduct theo ID của BranchProduct, shopId và branchId
    Optional<BranchProduct> findByIdAndShopIdAndBranchIdAndDeletedFalse(String id, String shopId, String branchId);

    // Tìm tất cả BranchProduct trong một shop và branch cụ thể
    Page<BranchProduct> findByShopIdAndBranchIdAndDeletedFalse(String shopId, String branchId, Pageable pageable);

    // Tìm tất cả BranchProduct trong một shop (không lọc theo branchId)
    Page<BranchProduct> findByShopIdAndDeletedFalse(String shopId, Pageable pageable);

    // Tìm các sản phẩm tồn kho thấp trong một branch
    List<BranchProduct> findByShopIdAndBranchIdAndQuantityLessThanAndDeletedFalse(String shopId, String branchId, int threshold);

    // Tìm các sản phẩm tồn kho thấp trên toàn shop (không lọc theo branchId)
    List<BranchProduct> findByShopIdAndQuantityLessThanAndDeletedFalse(String shopId, int threshold);

    // Tìm kiếm sản phẩm theo keyword trong tên hoặc danh mục (từ Product) và branchId
    // Đây sẽ là một query phức tạp hơn, cần join hoặc sử dụng aggregation.
    // Vì MongoRepository không hỗ trợ join trực tiếp giữa các collection,
    // chúng ta sẽ cần thực hiện lookup trong service hoặc dùng custom repository.
    // Tạm thời, chúng ta sẽ viết query tìm kiếm trên BranchProduct và để service join với Product.
    // Hoặc nếu muốn tìm kiếm trực tiếp, cần sử dụng @Query với Aggregation.
    // Giả định chúng ta sẽ tìm kiếm theo productId và sau đó service sẽ lọc theo keyword từ Product.
    // Hoặc tạo một query để tìm kiếm dựa trên name/category của master Product.

    // Đối với tìm kiếm, chúng ta sẽ cần dùng aggregation trong service hoặc một custom repository
    // để join BranchProduct với Product. Hoặc, nếu tên/danh mục có thể nằm trên BranchProduct (như mô hình cũ),
    // thì query sẽ đơn giản hơn. Nhưng trong mô hình mới, name/category nằm ở Product.
    // Để giữ các repository đơn giản, các hàm tìm kiếm phức tạp (liên quan đến join/lookup)
    // sẽ được xây dựng trong Service hoặc một Custom Repository Impl.
    // Vì vậy, tôi sẽ để các query đơn giản ở đây, và bổ sung logic join trong ProductServiceImpl.

    // Tìm tất cả BranchProduct theo productId
    List<BranchProduct> findByProductIdAndDeletedFalse(String productId);

    Page<BranchProduct> findByProductIdInAndShopIdAndBranchIdAndDeletedFalse(Set<String> productIds, String shopId, String branchId, Pageable pageable);

    Page<BranchProduct> findByProductIdInAndShopIdAndDeletedFalse(Set<String> productIds, String shopId, Pageable pageable);
}