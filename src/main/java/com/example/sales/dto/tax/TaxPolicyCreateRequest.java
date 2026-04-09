package com.example.sales.dto.tax;

import com.example.sales.constant.TaxRuleType;
import com.example.sales.model.tax.TaxRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxPolicyCreateRequest {

    @NotBlank
    private String name;

    /**
     * null / blank = áp dụng cả shop; có giá trị = chỉ chi nhánh đó
     */
    private String branchId;

    private boolean priceIncludesTax;

    @NotNull
    @Valid
    private List<TaxRuleItemRequest> rules;

    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;

    private Integer priority;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxRuleItemRequest {

        private String code;
        @NotBlank
        private String label;
        @NotNull
        private TaxRuleType type;
        /**
         * PERCENT: ví dụ 0.1 = 10%; FIXED: số tiền
         */
        private double value;
        @Builder.Default
        private boolean applyOnPreviousTaxes = false;
    }

    public List<TaxRule> toTaxRules() {
        if (rules == null) {
            return List.of();
        }
        return rules.stream()
                .map(r -> TaxRule.builder()
                        .code(r.getCode())
                        .label(r.getLabel())
                        .type(r.getType())
                        .value(r.getValue())
                        .applyOnPreviousTaxes(r.isApplyOnPreviousTaxes())
                        .required(true)
                        .build())
                .toList();
    }
}
