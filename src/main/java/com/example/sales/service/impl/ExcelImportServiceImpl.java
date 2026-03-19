// File: src/main/java/com/example/sales/service/impl/ExcelImportServiceImpl.java
package com.example.sales.service.impl;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.AppConstants;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Branch;
import com.example.sales.model.BranchProduct;
import com.example.sales.model.Product;
import com.example.sales.model.Shop;
import com.example.sales.repository.BranchProductRepository;
import com.example.sales.repository.BranchRepository;
import com.example.sales.repository.ProductRepository;
import com.example.sales.repository.ShopRepository;
import com.example.sales.service.ExcelImportService;
import com.example.sales.service.SequenceService;
import com.example.sales.util.CategoryUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Import sản phẩm từ Excel.
 *
 * Cấu trúc cột (0-based) — phải khớp với ExcelExportService:
 *   0  SKU                (tùy chọn — tự động sinh nếu để trống)
 *   1  Tên sản phẩm       (bắt buộc)
 *   2  Danh mục
 *   3  Đơn vị             (bắt buộc)
 *   4  Barcode
 *   5  Giá nhập mặc định  (Product.costPrice)
 *   6  Giá bán mặc định   (Product.defaultPrice)
 *   7  Giá bán chi nhánh  (BranchProduct.price)
 *   8  Giá nhập chi nhánh (BranchProduct.branchCostPrice)
 *   9  Số lượng           (BranchProduct.quantity)
 *   10 Số lượng tối thiểu (BranchProduct.minQuantity)
 *   11 Giá khuyến mãi     (BranchProduct.discountPrice)
 *   12 % Giảm giá         (BranchProduct.discountPercentage)
 *   13 Hạn sử dụng        (BranchProduct.expiryDate, yyyy-MM-dd)
 *   14 Mô tả              (Product.description)
 *   15 Trạng thái SP      (Product.active, TRUE/FALSE)
 *   16 Trạng thái chi nhánh (BranchProduct.activeInBranch, TRUE/FALSE)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportServiceImpl implements ExcelImportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ProductRepository productRepository;
    private final BranchProductRepository branchProductRepository;
    private final ShopRepository shopRepository;
    private final SequenceService sequenceService;
    private final BranchRepository branchRepository;

    @Override
    @Transactional
    public int importProducts(String shopId, InputStream inputStream) {
        // Load shop một lần — cần để sinh SKU theo đúng prefix industry/category
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        // Lấy tất cả branch active của shop
        List<Branch> activeBranches = branchRepository.findAllByShopIdAndDeletedFalse(shopId);
        if (activeBranches.isEmpty()) {
            log.warn("Shop '{}' không có chi nhánh active nào!", shopId);
            return 0;
        }
        int importedCount = 0;
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = findProductSheet(workbook);

            // Row 0 = header, Row 1 = ghi chú ("* Cột đỏ = bắt buộc"), Row 2+ = data
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    // ── Product fields ────────────────────────────────────────────
                    String sku          = getCellValue(row.getCell(0));
                    String name         = getCellValue(row.getCell(1));
                    // Lấy value trước dấu '-' nếu có, ví dụ: "value-label" -> "value"
                    String rawCategory  = getCellValue(row.getCell(2));
                    String category     = null;
                    if (rawCategory != null) {
                        int dashIdx = rawCategory.indexOf('-');
                        category = dashIdx > 0 ? rawCategory.substring(0, dashIdx).trim() : rawCategory.trim();
                        category = CategoryUtils.normalize(category);
                    }
                    // Lấy value trước dấu '-' nếu có, ví dụ: "value-label" -> "value"
                    String rawUnit = getCellValue(row.getCell(3));
                    String unit = null;
                    if (rawUnit != null) {
                        int dashIdx = rawUnit.indexOf('-');
                        unit = dashIdx > 0 ? rawUnit.substring(0, dashIdx).trim() : rawUnit.trim();
                    }
                    String barcode      = getCellValue(row.getCell(4));
                    double costPrice    = getNumericCellValue(row.getCell(5));
                    double defaultPrice = getNumericCellValue(row.getCell(6));
                    String description  = getCellValue(row.getCell(14));

                    // ── BranchProduct fields ──────────────────────────────────────
                    double branchPrice       = getNumericCellValue(row.getCell(7));
                    double branchCostPrice   = getNumericCellValue(row.getCell(8));
                    int    quantity          = (int) getNumericCellValue(row.getCell(9));
                    int    minQuantity       = (int) getNumericCellValue(row.getCell(10));
                    Double discountPrice     = getNullableNumeric(row.getCell(11));
                    Double discountPct       = getNullableNumeric(row.getCell(12));
                    LocalDate expiryDate     = getDateCellValue(row.getCell(13));

                    // Tên sản phẩm và danh mục là bắt buộc
                    // (danh mục cần thiết để sinh đúng prefix SKU: {INDUSTRY}_{CATEGORY}_XXX)
                    if (!StringUtils.hasText(name) || !StringUtils.hasText(category)) {
                        log.warn("Bỏ qua dòng {}: Thiếu tên sản phẩm hoặc danh mục.", i + 1);
                        continue;
                    }
                    if (!StringUtils.hasText(unit)) unit = "cái";
                    if (defaultPrice <= 0) defaultPrice = branchPrice > 0 ? branchPrice : 1;
                    if (branchPrice <= 0) branchPrice = defaultPrice;

                    // ── 1. Upsert Product ─────────────────────────────────────────
                    boolean skuProvided = StringUtils.hasText(sku);
                    Optional<Product> existingProduct = skuProvided
                            ? productRepository.findByShopIdAndSkuAndDeletedFalse(shopId, sku)
                            : Optional.empty();

                    Product product;
                    if (existingProduct.isPresent()) {
                        // SKU đã tồn tại → cập nhật thông tin chung
                        product = existingProduct.get();
                        product.setName(name);
                        product.setCategory(category);
                        product.setUnit(unit);
                        product.setDescription(description);
                        if (StringUtils.hasText(barcode)) product.setBarcode(barcode);
                        if (costPrice >= 0) product.setCostPrice(costPrice);
                        if (defaultPrice > 0) product.setDefaultPrice(defaultPrice);
                        product.setActive(true);
                        productRepository.save(product);
                        log.info("Cập nhật Product cho SKU '{}'", sku);
                    } else {
                        // SKU để trống hoặc không tồn tại → tạo mới với SKU auto-gen nếu cần
                        if (!skuProvided) {
                            String prefix = generateSkuPrefix(shop, category);
                            sku = sequenceService.getNextCode(shopId, prefix, AppConstants.SequenceTypes.SEQUENCE_TYPE_SKU);
                        }
                        product = Product.builder()
                                .shopId(shopId)
                                .sku(sku)
                                .name(name)
                                .category(category)
                                .unit(unit)
                                .description(description)
                                .barcode(StringUtils.hasText(barcode) ? barcode : null)
                                .costPrice(costPrice)
                                .defaultPrice(defaultPrice)
                                .build();
                        product = productRepository.save(product);
                        // Cập nhật sequence sau khi lưu thành công (y hệt ProductServiceImpl)
                        String prefix = generateSkuPrefix(shop, category);
                        sequenceService.updateNextSequence(shopId, prefix, AppConstants.SequenceTypes.SEQUENCE_TYPE_SKU);
                        log.info("Tạo mới Product với SKU '{}'", product.getSku());
                    }

                    // ── 2. Upsert BranchProduct cho tất cả branch active ──────────
                    for (Branch branch : activeBranches) {
                        String branchId = branch.getId();
                        Optional<BranchProduct> existingBP =
                                branchProductRepository.findByProductIdAndBranchIdAndDeletedFalse(product.getId(), branchId);
                        if (existingBP.isPresent()) {
                            BranchProduct bp = existingBP.get();
                            bp.setPrice(branchPrice);
                            bp.setBranchCostPrice(branchCostPrice);
                            bp.setQuantity(quantity);
                            bp.setMinQuantity(minQuantity);
                            bp.setDiscountPrice(discountPrice);
                            bp.setDiscountPercentage(discountPct);
                            bp.setExpiryDate(expiryDate);
                            branchProductRepository.save(bp);
                            log.info("Cập nhật BranchProduct cho SKU '{}' tại chi nhánh '{}'", product.getSku(), branchId);
                        } else {
                            BranchProduct bp = BranchProduct.builder()
                                    .productId(product.getId())
                                    .shopId(shopId)
                                    .branchId(branchId)
                                    .price(branchPrice)
                                    .branchCostPrice(branchCostPrice)
                                    .quantity(quantity)
                                    .minQuantity(minQuantity)
                                    .discountPrice(discountPrice)
                                    .discountPercentage(discountPct)
                                    .expiryDate(expiryDate)
                                    .build();
                            branchProductRepository.save(bp);
                            log.info("Tạo mới BranchProduct cho SKU '{}' tại chi nhánh '{}'", product.getSku(), branchId);
                        }
                    }
                    importedCount++;
                } catch (Exception e) {
                    log.error("Lỗi khi xử lý dòng {}: {}", i + 1, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Lỗi đọc file Excel: {}", e.getMessage());
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        return importedCount;
    }

    /**
     * Tìm đúng sheet chứa dữ liệu sản phẩm.
     * File Excel mẫu thường có một sheet ẩn ở đầu chứa source data cho dropdown
     * (danh mục, đơn vị...) — cần bỏ qua sheet đó.
     *
     * Ưu tiên theo thứ tự:
     *   1. Sheet tên "Products" (khớp với ExcelExportService)
     *   2. Sheet visible đầu tiên có header đúng (col 0 = "SKU" hoặc col 1 = "Tên sản phẩm")
     *   3. Sheet visible đầu tiên bất kỳ
     *   4. Sheet index 0 (last resort)
     */
    private Sheet findProductSheet(Workbook workbook) {
        // 1. Sheet tên "Products"
        Sheet byName = workbook.getSheet("Products");
        if (byName != null) return byName;

        Sheet firstVisible = null;
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            if (workbook.isSheetHidden(i)) continue;

            Sheet s = workbook.getSheetAt(i);
            if (firstVisible == null) firstVisible = s;

            // 2. Kiểm tra header row: col 0 = "SKU" hoặc col 1 chứa "Tên"
            Row headerRow = s.getRow(0);
            if (headerRow != null) {
                String col0 = getCellValue(headerRow.getCell(0));
                String col1 = getCellValue(headerRow.getCell(1));
                boolean looksLikeProductSheet =
                        "SKU".equalsIgnoreCase(col0) ||
                        (col1 != null && col1.toLowerCase().contains("tên"));
                if (looksLikeProductSheet) {
                    log.debug("Tìm thấy product sheet: '{}' (index {})", s.getSheetName(), i);
                    return s;
                }
            }
        }

        // 3. Sheet visible đầu tiên
        if (firstVisible != null) {
            log.warn("Không tìm thấy sheet theo header, dùng sheet visible đầu tiên: '{}'", firstVisible.getSheetName());
            return firstVisible;
        }

        // 4. Last resort
        log.warn("Không tìm thấy sheet visible, dùng sheet index 0");
        return workbook.getSheetAt(0);
    }

    /**
     * Sinh prefix SKU — delegate sang {@link CategoryUtils#toSkuSegment}.
     * Ví dụ: shop RETAIL + "Cá Tươi" → "RETAIL_CA_TUOI"
     */
    private String generateSkuPrefix(Shop shop, String category) {
        return StringUtils.hasText(category)
                ? String.format("%s_%s", shop.getType().getIndustry().name().toUpperCase(), CategoryUtils.toSkuSegment(category))
                : shop.getType().getIndustry().name().toUpperCase();
    }

    // ── Cell helpers ──────────────────────────────────────────────────────────

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default      -> null;
        };
    }

    private double getNumericCellValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return 0.0;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        if (cell.getCellType() == CellType.STRING) {
            try { return Double.parseDouble(cell.getStringCellValue().trim()); }
            catch (NumberFormatException e) { return 0.0; }
        }
        return 0.0;
    }

    private Double getNullableNumeric(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        double v = getNumericCellValue(cell);
        return v == 0.0 ? null : v;
    }

    private boolean getBooleanCellValue(Cell cell, boolean defaultValue) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return defaultValue;
        if (cell.getCellType() == CellType.BOOLEAN) return cell.getBooleanCellValue();
        if (cell.getCellType() == CellType.STRING) {
            String v = cell.getStringCellValue().trim().toUpperCase();
            return "TRUE".equals(v) || "CÓ".equals(v) || "1".equals(v);
        }
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue() != 0;
        return defaultValue;
    }

    private LocalDate getDateCellValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        if (cell.getCellType() == CellType.STRING) {
            String s = cell.getStringCellValue().trim();
            if (!StringUtils.hasText(s)) return null;
            try { return LocalDate.parse(s, DATE_FORMATTER); }
            catch (DateTimeParseException e) {
                log.warn("Không thể parse ngày '{}', bỏ qua trường expiryDate.", s);
                return null;
            }
        }
        return null;
    }
}

