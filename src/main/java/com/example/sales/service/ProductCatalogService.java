package com.example.sales.service;

import com.example.sales.dto.product.ProductCatalogResponse;

import java.util.List;
import java.util.Optional;

public interface ProductCatalogService {

    /**
     * Upsert thông tin sản phẩm vào internal catalog theo barcode.
     * Gọi sau khi tạo hoặc cập nhật sản phẩm có barcode.
     * Nếu barcode đã tồn tại → cập nhật name/category/description/images.
     * Nếu chưa tồn tại → tạo mới.
     *
     * @param barcode     Mã barcode của sản phẩm (bỏ qua nếu blank)
     * @param name        Tên sản phẩm
     * @param category    Danh mục (đã được normalize qua CategoryUtils)
     * @param description Mô tả sản phẩm
     * @param images      Danh sách URL ảnh
     */
    void upsert(String barcode, String name, String category, String description, List<String> images);

    /**
     * Tra cứu thông tin catalog theo barcode.
     *
     * @param barcode Mã barcode cần tra cứu
     * @return Optional chứa thông tin catalog, empty nếu chưa có trong catalog
     */
    Optional<ProductCatalogResponse> findByBarcode(String barcode);
}

