package com.example.sales.model.tax;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTaxSnapshot {

    /**
     * Giá đã bao gồm thuế hay chưa
     */
    private boolean priceIncludesTax;

    /**
     * Tổng tiền chưa thuế (NET)
     */
    private double netAmount;

    /**
     * Tổng tiền thuế
     */
    private double taxTotal;

    /**
     * Tổng tiền phải thanh toán (GROSS)
     */
    private double grandTotal;

    /**
     * Chi tiết từng loại thuế
     */
    private List<OrderTaxLine> taxes;
}
