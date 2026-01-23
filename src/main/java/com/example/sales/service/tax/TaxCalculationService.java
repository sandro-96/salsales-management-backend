package com.example.sales.service.tax;

import com.example.sales.constant.TaxRuleType;
import com.example.sales.model.tax.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaxCalculationService {

    /**
     * @param baseAmount
     *  - Nếu priceIncludesTax = true → đây là GROSS
     *  - Nếu priceIncludesTax = false → đây là NET
     */
    public OrderTaxSnapshot calculate(double baseAmount, TaxPolicy policy) {

        boolean priceIncludesTax = policy.isPriceIncludesTax();
        List<OrderTaxLine> taxLines = new ArrayList<>();

        double netAmount;
        double runningBase;
        double taxTotal = 0;

        // 1️⃣ Xác định NET
        if (priceIncludesTax) {
            netAmount = extractNetAmount(baseAmount, policy);
            runningBase = netAmount;
        } else {
            netAmount = baseAmount;
            runningBase = baseAmount;
        }

        // 2️⃣ Tính từng TaxRule
        for (TaxRule rule : policy.getRules()) {

            double taxableBase = rule.isApplyOnPreviousTaxes()
                    ? runningBase
                    : netAmount;

            double taxAmount = calculateTax(rule, taxableBase);

            taxLines.add(OrderTaxLine.builder()
                    .code(rule.getCode())
                    .label(rule.getLabel())
                    .rate(rule.getType() == TaxRuleType.PERCENT ? rule.getValue() : 0)
                    .amount(taxAmount)
                    .build()
            );

            taxTotal += taxAmount;
            runningBase += taxAmount;
        }

        double grandTotal = netAmount + taxTotal;

        return OrderTaxSnapshot.builder()
                .priceIncludesTax(priceIncludesTax)
                .netAmount(netAmount)
                .taxTotal(taxTotal)
                .grandTotal(grandTotal)
                .taxes(taxLines)
                .build();
    }

    /**
     * Tách NET từ GROSS
     * (chỉ áp dụng cho thuế % không chồng thuế)
     */
    private double extractNetAmount(double gross, TaxPolicy policy) {
        double percentSum = policy.getRules().stream()
                .filter(r -> r.getType() == TaxRuleType.PERCENT)
                .filter(r -> !r.isApplyOnPreviousTaxes())
                .mapToDouble(TaxRule::getValue)
                .sum();

        return percentSum > 0
                ? gross / (1 + percentSum)
                : gross;
    }

    private double calculateTax(TaxRule rule, double base) {
        return switch (rule.getType()) {
            case PERCENT -> base * rule.getValue();
            case FIXED -> rule.getValue();
        };
    }
}
