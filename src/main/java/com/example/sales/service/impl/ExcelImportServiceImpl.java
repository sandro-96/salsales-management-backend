// File: src/main/java/com/example/sales/service/impl/ExcelImportServiceImpl.java
package com.example.sales.service.impl;

import com.example.sales.constant.ApiCode;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.BranchProduct;
import com.example.sales.model.Product;
import com.example.sales.repository.BranchProductRepository;
import com.example.sales.repository.ProductRepository;
import com.example.sales.service.ExcelImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportServiceImpl implements ExcelImportService {

    private final ProductRepository productRepository;
    private final BranchProductRepository branchProductRepository;

    @Override
    @Transactional
    public int importProducts(String shopId, String branchId, InputStream inputStream) {
        int importedCount = 0;
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0); // Lấy sheet đầu tiên

            // Bỏ qua hàng tiêu đề
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    // Đọc dữ liệu từ các cột
                    String sku = getCellValue(row.getCell(0)); // Giả định SKU ở cột 0
                    String name = getCellValue(row.getCell(1)); // Giả định Tên ở cột 1
                    String category = getCellValue(row.getCell(2)); // Giả định Danh mục ở cột 2
                    String unit = getCellValue(row.getCell(3)); // Giả định Đơn vị ở cột 3
                    double price = getNumericCellValue(row.getCell(4)); // Giả định Giá ở cột 4
                    int quantity = (int) getNumericCellValue(row.getCell(5)); // Giả định Số lượng ở cột 5
                    String imageUrl = getCellValue(row.getCell(6)); // Giả định Ảnh ở cột 6
                    String description = getCellValue(row.getCell(7)); // Giả định Mô tả ở cột 7

                    if (!StringUtils.hasText(sku) || !StringUtils.hasText(name) || !StringUtils.hasText(category) || !StringUtils.hasText(unit)) {
                        log.warn("Bỏ qua dòng {}: Thiếu SKU, tên, danh mục hoặc đơn vị.", i + 1);
                        continue;
                    }

                    // 1. Tìm hoặc tạo Product (định nghĩa chung)
                    Optional<Product> existingProduct = productRepository.findByShopIdAndSkuAndDeletedFalse(shopId, sku);
                    Product product;
                    if (existingProduct.isPresent()) {
                        product = existingProduct.get();
                        // Cập nhật thông tin chung nếu có thay đổi
                        product.setName(name);
                        product.setCategory(category);
                        productRepository.save(product);
                    } else {
                        product = Product.builder()
                                .shopId(shopId)
                                .name(name)
                                .category(category)
                                .sku(sku)
                                .build();
                        product = productRepository.save(product);
                    }

                    // 2. Tạo hoặc cập nhật BranchProduct
                    Optional<BranchProduct> existingBranchProduct = branchProductRepository.findByProductIdAndBranchIdAndDeletedFalse(product.getId(), branchId);
                    BranchProduct branchProduct;
                    if (existingBranchProduct.isPresent()) {
                        branchProduct = existingBranchProduct.get();
                        branchProduct.setQuantity(quantity);
                        branchProduct.setPrice(price);
                        branchProduct.setUnit(unit);
                        branchProduct.setImageUrl(imageUrl);
                        branchProduct.setDescription(description);
                        branchProduct.setActiveInBranch(true); // Mặc định kích hoạt khi nhập
                        branchProductRepository.save(branchProduct);
                        log.info("Cập nhật BranchProduct cho SKU '{}' tại chi nhánh '{}'", sku, branchId);
                    } else {
                        branchProduct = BranchProduct.builder()
                                .productId(product.getId())
                                .shopId(shopId)
                                .branchId(branchId)
                                .quantity(quantity)
                                .price(price)
                                .unit(unit)
                                .imageUrl(imageUrl)
                                .description(description)
                                .activeInBranch(true) // Mặc định kích hoạt khi nhập
                                .build();
                        branchProductRepository.save(branchProduct);
                        log.info("Tạo mới BranchProduct cho SKU '{}' tại chi nhánh '{}'", sku, branchId);
                    }
                    importedCount++;
                } catch (Exception e) {
                    log.error("Lỗi khi xử lý dòng {}: {}", i + 1, e.getMessage());
                    // Có thể throw BusinessException hoặc thu thập các lỗi để trả về thông báo chi tiết
                }
            }
        } catch (IOException e) {
            log.error("Lỗi đọc file Excel: {}", e.getMessage());
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        return importedCount;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue()); // For numeric cells that should be string (e.g., SKU if it's numeric)
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            case BLANK -> null;
            default -> null;
        };
    }

    private double getNumericCellValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return 0.0;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue());
            } catch (NumberFormatException e) {
                return 0.0; // Hoặc throw exception nếu giá trị không hợp lệ
            }
        }
        return 0.0;
    }
}