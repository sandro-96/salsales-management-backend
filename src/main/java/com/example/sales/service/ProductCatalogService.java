package com.example.sales.service;

import com.example.sales.dto.product.ProductCatalogUpsertRequest;
import com.example.sales.dto.product.ProductCatalogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductCatalogService {

    /**
     * Tạo hoặc cập nhật catalog chuẩn — chỉ gọi từ luồng system admin.
     * Barcode được chuẩn hoá / kiểm tra GS1 giống lưu sản phẩm.
     */
    ProductCatalogResponse upsertFromAdmin(ProductCatalogUpsertRequest request);

    /**
     * Tra cứu thông tin catalog theo barcode.
     *
     * @param barcode Mã barcode cần tra cứu
     * @return Optional chứa thông tin catalog, empty nếu chưa có trong catalog
     */
    Optional<ProductCatalogResponse> findByBarcode(String barcode);

    /**
     * Tìm kiếm theo tên (substring, không phân biệt hoa thường) — dùng khi shop gõ tên để gợi ý từ catalog chuẩn.
     */
    List<ProductCatalogResponse> searchByNameKeyword(String keyword, int limit);

    /**
     * Liệt kê catalog có lọc theo keyword (match vào name hoặc barcode) và category.
     * Dùng cho admin catalog page.
     */
    Page<ProductCatalogResponse> list(String keyword, String category, Pageable pageable);

    /**
     * Xoá một entry catalog theo id. Gọi từ admin UI.
     */
    void deleteById(String id);
}

