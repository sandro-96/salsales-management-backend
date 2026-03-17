package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Internal product catalog — bảng tra cứu thông tin sản phẩm theo barcode.
 *
 * Khi bất kỳ shop nào tạo / cập nhật sản phẩm có barcode, hệ thống tự động
 * upsert record vào đây. Các shop khác có thể tra cứu barcode để lấy thông tin
 * gợi ý khi nhập sản phẩm mới, giúp tiết kiệm thời gian nhập liệu.
 *
 * Last-write-wins: mỗi lần upsert sẽ overwrite name/category/description/images
 * bằng thông tin mới nhất từ shop vừa lưu.
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

