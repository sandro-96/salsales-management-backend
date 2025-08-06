// File: src/main/java/com/example/sales/service/InventoryService.java
package com.example.sales.service;

import com.example.sales.dto.inventory.InventoryTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    /**
     * Nhập thêm số lượng sản phẩm vào kho của chi nhánh.
     * Tạo một InventoryTransaction loại IMPORT.
     *
     * @param userId ID của người dùng thực hiện.
     * @param shopId ID của cửa hàng.
     * @param branchProductId ID của BranchProduct cần nhập.
     * @param quantity Số lượng nhập thêm.
     * @param note Ghi chú cho giao dịch.
     * @return Số lượng tồn kho mới của sản phẩm tại chi nhánh.
     */
    int importProductQuantity(String userId, String shopId, String branchId, String branchProductId, int quantity, String note);

    /**
     * Xuất bớt số lượng sản phẩm khỏi kho của chi nhánh.
     * Tạo một InventoryTransaction loại EXPORT.
     * Kiểm tra số lượng tồn kho trước khi xuất.
     *
     * @param userId ID của người dùng thực hiện.
     * @param shopId ID của cửa hàng.
     * @param branchProductId ID của BranchProduct cần xuất.
     * @param quantity Số lượng xuất đi.
     * @param note Ghi chú cho giao dịch.
     * @param referenceId ID tham chiếu (ví dụ: ID đơn hàng).
     * @return Số lượng tồn kho mới của sản phẩm tại chi nhánh.
     */
    int exportProductQuantity(String userId, String shopId, String branchId, String branchProductId, int quantity, String note, String referenceId);

    /**
     * Điều chỉnh số lượng tồn kho của sản phẩm tại chi nhánh.
     * Có thể tăng hoặc giảm số lượng, tạo một InventoryTransaction loại ADJUSTMENT.
     *
     * @param userId ID của người dùng thực hiện.
     * @param shopId ID của cửa hàng.
     * @param branchProductId ID của BranchProduct cần điều chỉnh.
     * @param newQuantity Số lượng tồn kho mong muốn sau điều chỉnh.
     * @param note Ghi chú cho giao dịch.
     * @return Số lượng tồn kho mới của sản phẩm tại chi nhánh.
     */
    int adjustProductQuantity(String userId, String shopId, String branchId, String branchProductId, int newQuantity, String note);

    /**
     * Lấy lịch sử giao dịch tồn kho cho một sản phẩm cụ thể.
     *
     * @param branchProductId ID của BranchProduct (sản phẩm chi nhánh).
     * @param pageable Thông tin phân trang.
     * @return Trang chứa danh sách các giao dịch tồn kho.
     */
    Page<InventoryTransactionResponse> getTransactionHistory(String userId, String shopId, String branchId, String branchProductId, Pageable pageable);

    /**
     * Kiểm tra xem cửa hàng có yêu cầu quản lý tồn kho hay không.
     *
     * @param shopId ID của cửa hàng.
     * @return true nếu cửa hàng yêu cầu quản lý tồn kho, ngược lại false.
     */
    boolean isInventoryManagementRequired(String shopId);
}