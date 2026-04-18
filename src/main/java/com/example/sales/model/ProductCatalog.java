package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Product catalog chuẩn hoá theo barcode — dữ liệu do system admin quản lý.
 *
 * Shop chỉ tra cứu (read) qua API catalog để gợi ý khi tạo sản phẩm; không ghi
 * vào collection này khi lưu sản phẩm hay import Excel.
 *
 * Last-write-wins: mỗi lần admin cập nhật sẽ ghi đè name/category/description/images.
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("product_catalog")
public class ProductCatalog extends BaseEntity {

    @Id
    private String id;

    /**
     * Barcode của sản phẩm — unique index, dùng làm khóa tra cứu.
     */
    @Indexed(unique = true)
    private String barcode;

    private String name;
    private String category;
    private String description;
    private List<String> images;
}

