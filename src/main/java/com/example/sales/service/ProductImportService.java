// File: src/main/java/com/example/sales/service/ProductImportService.java
package com.example.sales.service;

import com.example.sales.dto.product.ProductRequest;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductImportService {

    private final ProductService productService;

    public Map<String, Object> importExcel(String shopId, String branchId, MultipartFile file) {
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            int rows = sheet.getPhysicalNumberOfRows();

            for (int i = 1; i < rows; i++) {
                Row row = sheet.getRow(i);
                try {
                    ProductRequest req = parseRow(row);
                    req.setBranchId(branchId);
                    productService.createProduct(shopId, req);
                    successCount++;
                } catch (Exception ex) {
                    failCount++;
                    errors.add("Dòng " + (i + 1) + ": " + ex.getMessage());
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Không thể đọc file Excel", e);
        }

        return Map.of(
                "success", successCount,
                "failed", failCount,
                "errors", errors
        );
    }

    private ProductRequest parseRow(Row row) {
        ProductRequest req = new ProductRequest();
        req.setName(getString(row, 0));
        req.setProductCode(getString(row, 1));
        req.setCategory(getString(row, 2));
        req.setQuantity(getInt(row, 3));
        req.setPrice(getDouble(row, 4));
        req.setUnit(getString(row, 5));
        req.setImageUrl(getString(row, 6));
        req.setDescription(getString(row, 7));

        String status = getString(row, 8).toLowerCase();
        req.setActive(!status.contains("ngưng"));

        return req;
    }

    private String getString(Row row, int i) {
        Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        return cell.toString().trim();
    }

    private int getInt(Row row, int i) {
        try {
            return (int) Double.parseDouble(getString(row, i));
        } catch (Exception e) {
            return 0;
        }
    }

    private double getDouble(Row row, int i) {
        try {
            return Double.parseDouble(getString(row, i));
        } catch (Exception e) {
            return 0;
        }
    }
}
