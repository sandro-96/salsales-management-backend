package com.example.sales.model.tax;

import com.example.sales.constant.TaxRuleType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxRule {

    /**
     * VAT, SERVICE_FEE, ENV_FEE
     */
    private String code;

    /**
     * Hiển thị cho hóa đơn
     */
    private String label;

    private TaxRuleType type; // PERCENT / FIXED
    private double value;     // 0.1 hoặc 2000

    /**
     * Thuế chồng thuế?
     * true: tính trên (price + các thuế trước)
     */
    @Builder.Default
    private boolean applyOnPreviousTaxes = false;

    /**
     * Có bắt buộc hay không
     */
    @Builder.Default
    private boolean required = true;
}
