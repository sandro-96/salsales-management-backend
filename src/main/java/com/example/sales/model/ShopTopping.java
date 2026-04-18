package com.example.sales.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Topping cấp shop — giá chung, có thể gán cho nhiều sản phẩm.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopTopping {

    private String toppingId;

    private String name;

    @Builder.Default
    private double extraPrice = 0;

    @Builder.Default
    private boolean active = true;
}
