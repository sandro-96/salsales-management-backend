package com.example.sales.service.tax;

import com.example.sales.model.Order;
import com.example.sales.model.tax.OrderTaxSnapshot;
import com.example.sales.model.tax.TaxPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderTaxApplier {

    private final TaxPolicyService taxPolicyService;
    private final TaxCalculationService taxCalculationService;

    /**
     * Áp tax cho order (khi tạo / preview)
     */
    public void applyTax(Order order) {

        TaxPolicy policy = taxPolicyService
                .findEffectivePolicy(order.getShopId(), order.getBranchId(), LocalDateTime.now())
                .orElseGet(TaxPolicy::noTaxFallback);

        double baseAmount = order.getTotalPrice();
        // totalPrice = tổng item trước tax (design của bạn đang rất ổn)

        OrderTaxSnapshot snapshot =
                taxCalculationService.calculate(baseAmount, policy);

        order.setTaxSnapshot(snapshot);
        order.setTotalAmount(snapshot.getGrandTotal());
    }

    /**
     * Xem trước thuế theo tổng tiền hàng (sau KM / giảm điểm — cùng nghĩa với totalPrice khi tạo đơn).
     */
    public OrderTaxSnapshot preview(String shopId, String branchId, double baseAmount) {
        TaxPolicy policy = taxPolicyService
                .findEffectivePolicy(shopId, branchId, LocalDateTime.now())
                .orElseGet(TaxPolicy::noTaxFallback);
        return taxCalculationService.calculate(Math.max(0, baseAmount), policy);
    }
}
