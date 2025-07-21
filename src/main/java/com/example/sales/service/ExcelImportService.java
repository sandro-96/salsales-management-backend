// File: src/main/java/com/example/sales/service/ExcelImportService.java
package com.example.sales.service;

import java.io.InputStream;

public interface ExcelImportService {
    /**
     * Nhập sản phẩm từ file Excel vào một chi nhánh cụ thể của một cửa hàng.
     * Mỗi dòng trong file Excel sẽ tạo/cập nhật một BranchProduct và định nghĩa Product chung nếu cần.
     *
     * @param shopId ID của cửa hàng.
     * @param branchId ID của chi nhánh mà sản phẩm sẽ được nhập vào.
     * @param inputStream InputStream của file Excel.
     * @return Số lượng sản phẩm (BranchProduct) được nhập/cập nhật thành công.
     */
    int importProducts(String shopId, String branchId, InputStream inputStream);
}