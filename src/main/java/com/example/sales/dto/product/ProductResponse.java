// File: src/main/java/com/example/sales/dto/product/ProductResponse.java
package com.example.sales.dto.product;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin sản phẩm.
 */
@Data
@Builder
public class ProductResponse {
    /**
     * ID sản phẩm.
     */
    private String id;

    /**
     * Tên sản phẩm.
     */
    private String name;

    /**
     * Giá sản phẩm.
     */
    private double price;

    /**
     * Số lượng tồn kho.
     */
    private int quantity;

    /**
     * Danh mục sản phẩm.
     */
    private String category;

    /**
     * Mã sản phẩm.
     */
    private String sku;

    /**
     * URL hình ảnh sản phẩm.
     */
    private String imageUrl;

    /**
     * Mô tả sản phẩm.
     */
    private String description;

    /**
     * Trạng thái sản phẩm (kích hoạt hay không).
     */
    private boolean active;

    /**
     * Đơn vị sản phẩm (ví dụ: kg, lít).
     */
    private String unit;

    /**
     * Mã định danh sản phẩm.
     */
    private String productCode;

    /**
     * ID cửa hàng sở hữu sản phẩm.
     */
    private String shopId;

    /**
     * Thời gian tạo sản phẩm.
     */
    private LocalDateTime createdAt;

    /**
     * Thời gian cập nhật sản phẩm.
     */
    private LocalDateTime updatedAt;
}
