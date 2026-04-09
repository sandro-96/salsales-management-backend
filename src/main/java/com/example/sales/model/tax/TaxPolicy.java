package com.example.sales.model.tax;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tax_policies")
@CompoundIndex(
        name = "idx_shop_branch_active",
        def = "{'shopId':1,'branchId':1,'active':1,'effectiveFrom':1,'effectiveTo':1}"
)
public class TaxPolicy {

    @Id
    private String id;

    @Indexed
    private String shopId;

    /**
     * null = default cho shop
     * not null = override cho branch
     */
    @Indexed
    private String branchId;

    /**
     * Tên hiển thị: "VAT 10%", "VAT 8% - KCN"
     */
    private String name;

    /**
     * Giá sản phẩm đã bao gồm thuế hay chưa
     * VN: thường = true
     */
    private boolean priceIncludesTax;

    /**
     * Danh sách các loại thuế áp dụng
     */
    private List<TaxRule> rules;

    /**
     * Thời điểm hiệu lực (optional nhưng rất nên có)
     */
    private LocalDateTime effectiveFrom;
    /**
     * null = không có ngày kết thúc
     */
    private LocalDateTime effectiveTo;

    @Builder.Default
    private int priority = 0;

    @Builder.Default
    private int version = 1;

    @Builder.Default
    private boolean active = true;

    /**
     * Khi shop/chi nhánh chưa cấu hình chính sách thuế: không thuế, tổng thanh toán = tiền hàng.
     */
    public static TaxPolicy noTaxFallback() {
        return TaxPolicy.builder()
                .name("Không áp thuế (chưa cấu hình)")
                .priceIncludesTax(false)
                .rules(Collections.emptyList())
                .build();
    }
}
