package com.example.sales.model.tax;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTaxLine {

    private String code;   // VAT
    private String label;  // Thuế GTGT
    private double rate;   // 0.1
    private double amount; // số tiền thuế
}
