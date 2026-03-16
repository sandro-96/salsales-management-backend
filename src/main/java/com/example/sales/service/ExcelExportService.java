// File: src/main/java/com/example/sales/service/ExcelExportService.java
package com.example.sales.service;

import com.example.sales.cache.ProductCache;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.export.GenericExcelExporter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Xuất sản phẩm ra file Excel với cấu trúc cột nhất quán với ExcelImportService:
 *
 *   Col  0  SKU
 *   Col  1  Tên sản phẩm
 *   Col  2  Danh mục
 *   Col  3  Đơn vị
 *   Col  4  Barcode
 *   Col  5  Giá nhập mặc định   (Product.costPrice)
 *   Col  6  Giá bán mặc định    (Product.defaultPrice)
 *   Col  7  Giá bán chi nhánh   (BranchProduct.price)
 *   Col  8  Giá nhập chi nhánh  (BranchProduct.branchCostPrice)
 *   Col  9  Số lượng            (BranchProduct.quantity)
 *   Col 10  Số lượng tối thiểu  (BranchProduct.minQuantity)
 *   Col 11  Giá khuyến mãi      (BranchProduct.discountPrice)
 *   Col 12  % Giảm giá          (BranchProduct.discountPercentage)
 *   Col 13  Hạn sử dụng         (BranchProduct.expiryDate, yyyy-MM-dd)
 *   Col 14  Mô tả               (Product.description)
 *   Col 15  Trạng thái SP       (Product.active)
 *   Col 16  Trạng thái chi nhánh (BranchProduct.activeInBranch)
 */
@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final ProductCache productCache;

    private static final String PRODUCT_NOTE_ROW = "* Cột đỏ = bắt buộc (Tên sản phẩm, Danh mục)";

    private static final List<String> PRODUCT_HEADERS = List.of(
            "SKU",
            "Tên sản phẩm",
            "Danh mục",
            "Đơn vị",
            "Barcode",
            "Giá nhập mặc định",
            "Giá bán mặc định",
            "Giá bán chi nhánh",
            "Giá nhập chi nhánh",
            "Số lượng",
            "Số lượng tối thiểu",
            "Giá khuyến mãi",
            "% Giảm giá",
            "Hạn sử dụng",
            "Mô tả",
            "Trạng thái SP",
            "Trạng thái chi nhánh"
    );

    private static final Function<ProductResponse, List<String>> PRODUCT_ROW_MAPPER = p -> {
        List<String> row = new ArrayList<>();
        row.add(safe(p.getSku()));
        row.add(safe(p.getName()));
        row.add(safe(p.getCategory()));
        row.add(safe(p.getUnit()));
        row.add(safe(p.getBarcode()));
        row.add(String.valueOf(p.getCostPrice()));
        row.add(String.valueOf(p.getDefaultPrice()));
        // BranchProduct fields — may be 0 when exported at shop level (no branchId)
        row.add(p.getBranchId() != null ? String.valueOf(p.getPrice())          : "");
        row.add(p.getBranchId() != null ? String.valueOf(p.getBranchCostPrice()) : "");
        row.add(p.getBranchId() != null ? String.valueOf(p.getQuantity())        : "");
        row.add(p.getBranchId() != null ? String.valueOf(p.getMinQuantity())     : "");
        row.add(p.getBranchId() != null && p.getDiscountPrice()      != null ? String.valueOf(p.getDiscountPrice())      : "");
        row.add(p.getBranchId() != null && p.getDiscountPercentage() != null ? String.valueOf(p.getDiscountPercentage()) : "");
        row.add(p.getBranchId() != null && p.getExpiryDate()         != null ? p.getExpiryDate().toString()             : "");
        row.add(safe(p.getDescription()));
        row.add(p.isActive() ? "TRUE" : "FALSE");
        row.add(p.getBranchId() != null ? (p.isActiveInBranch() ? "TRUE" : "FALSE") : "");
        return row;
    };

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Xuất sản phẩm cho một chi nhánh cụ thể hoặc toàn shop (khi branchId == null).
     * Khi không có branchId, các cột chi nhánh (giá bán chi nhánh, số lượng...) sẽ để trống.
     */
    public ResponseEntity<byte[]> exportProducts(String shopId, String branchId) {
        List<ProductResponse> allProducts = fetchAll(shopId, branchId);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            GenericExcelExporter<ProductResponse> exporter = new GenericExcelExporter<>();
            exporter.export("Products", PRODUCT_HEADERS, PRODUCT_NOTE_ROW, allProducts, PRODUCT_ROW_MAPPER, bos);
            byte[] content = bos.toByteArray();

            String filename = StringUtils.hasText(branchId)
                    ? "products_branch_" + branchId + ".xlsx"
                    : "products_shop_" + shopId + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(filename).build().toString())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(content);
        } catch (Exception e) {
            throw new RuntimeException("Không thể export file Excel sản phẩm", e);
        }
    }

    /**
     * Phương thức exportExcel tổng quát — dùng được cho các entity khác ngoài Product.
     */
    public <T> ResponseEntity<byte[]> exportExcel(String fileName,
                                                   String sheetName,
                                                   List<String> headers,
                                                   List<T> data,
                                                   Function<T, List<String>> rowMapper) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            GenericExcelExporter<T> exporter = new GenericExcelExporter<>();
            exporter.export(sheetName, headers, data, rowMapper, bos);
            byte[] content = bos.toByteArray();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            responseHeaders.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());

            return new ResponseEntity<>(content, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            throw new RuntimeException("Không thể export file Excel", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private List<ProductResponse> fetchAll(String shopId, String branchId) {
        List<ProductResponse> result = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, 500);
        Page<ProductResponse> page;
        do {
            if (StringUtils.hasText(branchId)) {
                page = productCache.getAllByBranch(shopId, branchId, "", pageable);
            } else {
                page = productCache.getAllByShop(shopId, "", pageable);
            }
            result.addAll(page.getContent());
            pageable = page.hasNext() ? page.nextPageable() : null;
        } while (pageable != null);
        return result;
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}