package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("branch_products")
@CompoundIndexes({
        @CompoundIndex(def = "{'shopId': 1, 'branchId': 1, 'deleted': 1}", name = "idx_shop_branch_deleted"),
        @CompoundIndex(def = "{'shopId': 1, 'deleted': 1}", name = "idx_shop_deleted"),
        @CompoundIndex(def = "{'productId': 1, 'branchId': 1}", unique = true, name = "idx_product_branch_unique")
})
public class BranchProduct extends BaseEntity {
    @Id
    private String id;

    @NotBlank(message = "Product ID không được để trống")
    private String productId;

    @NotBlank(message = "Shop ID không được để trống")
    private String shopId;

    @NotBlank(message = "Branch ID không được để trống")
    private String branchId;

    @Min(value = 0, message = "Số lượng không được âm")
    private int quantity; // Số lượng tồn kho tại chi nhánh (dùng cho SP đếm đơn vị)

    @Min(value = 0, message = "Số lượng tối thiểu không được âm")
    private int minQuantity; // Số lượng tối thiểu để cảnh báo nhập hàng

    /**
     * Tồn kho tính theo đơn vị cơ sở (base unit) cho SP bán theo cân/volume:
     * gram với kg/g, ml với l/ml, hoặc đơn vị chuyên biệt. null = legacy, chưa migrate.
     * <p>
     * Dùng cho {@code Product.sellByWeight == true}. Khi đặt đơn, hệ thống sẽ
     * quy đổi {@code weight} ra base unit và trừ vào trường này thay cho {@code quantity}.
     */
    private Long stockInBaseUnits;

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá bán phải lớn hơn 0")
    private double price; // Giá bán tại chi nhánh

    @DecimalMin(value = "0.0", message = "Giá nhập không được âm")
    private double branchCostPrice; // Giá nhập tại chi nhánh

    private Double discountPrice; // Giá khuyến mãi
    private Double discountPercentage; // Phần trăm giảm giá

    private LocalDate expiryDate; // Hạn sử dụng (cho thực phẩm, thuốc)

    @Builder.Default
    private boolean activeInBranch = true; // Trạng thái tại chi nhánh

    private List<BranchProductVariant> variants; // Biến thể tại chi nhánh

    @Builder.Default
    private List<PriceHistory> priceHistory = new ArrayList<>(); // Lịch sử giá tại chi nhánh
}

