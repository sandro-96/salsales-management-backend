// File: src/main/java/com/example/sales/dto/product/ProductRequest.java
package com.example.sales.dto.product;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO yêu cầu tạo/cập nhật sản phẩm.
 */
@Getter
@Setter
public class ProductRequest {

    /**
     * Tên sản phẩm, không được để trống.
     */
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 100, message = "Tên sản phẩm không được vượt quá 100 ký tự")
    private String name;

    /**
     * Giá sản phẩm, phải là số dương.
     */
    @NotNull(message = "Giá sản phẩm là bắt buộc")
    @Positive(message = "Giá sản phẩm phải lớn hơn 0")
    private Double price;

    /**
     * Số lượng tồn kho, phải là số không âm.
     */
    @NotNull(message = "Số lượng tồn kho là bắt buộc")
    @PositiveOrZero(message = "Số lượng tồn kho phải là số không âm")
    private Integer quantity;

    /**
     * Danh mục sản phẩm, không được để trống.
     */
    @NotBlank(message = "Danh mục sản phẩm không được để trống")
    @Size(max = 50, message = "Danh mục không được vượt quá 50 ký tự")
    private String category;

    /**
     * Mã sản phẩm, nếu có, phải tuân theo định dạng (ví dụ: SKU-123).
     */
    @Pattern(regexp = "^[A-Z0-9-]{0,20}$", message = "Mã sản phẩm không hợp lệ")
    private String sku;

    /**
     * URL hình ảnh sản phẩm, nếu có.
     */
    @Size(max = 255, message = "URL hình ảnh không được vượt quá 255 ký tự")
    private String imageUrl;

    /**
     * Mô tả sản phẩm, nếu có.
     */
    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;

    /**
     * Đơn vị sản phẩm (ví dụ: kg, cái), nếu có.
     */
    @Size(max = 20, message = "Đơn vị không được vượt quá 20 ký tự")
    private String unit;

    /**
     * Trạng thái sản phẩm (mặc định là true).
     */
    private boolean active = true;

    /**
     * Mã định danh sản phẩm, nếu có.
     */
    @Size(max = 50, message = "Mã định danh sản phẩm không được vượt quá 50 ký tự")
    private String productCode;

    /**
     * ID chi nhánh, có thể để null nếu không thuộc chi nhánh cụ thể.
     */
    @Size(max = 50, message = "ID chi nhánh không được vượt quá 50 ký tự")
    private String branchId;
}
