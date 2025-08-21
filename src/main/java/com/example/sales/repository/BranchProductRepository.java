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

    // Tìm tất cả BranchProduct theo productId
    List<BranchProduct> findByProductIdAndDeletedFalse(String productId);

    Page<BranchProduct> findByProductIdInAndShopIdAndBranchIdAndDeletedFalse(Set<String> productIds, String shopId, String branchId, Pageable pageable);

    Page<BranchProduct> findByProductIdInAndShopIdAndDeletedFalse(Set<String> productIds, String shopId, Pageable pageable);

    List<BranchProduct> findByProductIdAndBranchIdInAndDeletedFalse(String productId, List<String> branchIds);
}