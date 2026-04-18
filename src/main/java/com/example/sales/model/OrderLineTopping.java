package com.example.sales.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Snapshot topping trên một dòng đơn (giá tại thời điểm đặt).
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderLineTopping {

    private String toppingId;
    private String name;
    /** Giá phụ một đơn vị sản phẩm (đã snapshot) */
    private double extraPrice;
}
