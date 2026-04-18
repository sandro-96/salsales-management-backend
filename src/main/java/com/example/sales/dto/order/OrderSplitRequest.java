package com.example.sales.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSplitRequest {
    /** Optional: move items to a new table (must be in same shop/branch) */
    private String toTableId;

    @Valid
    @NotEmpty(message = "itemsToMove không được để trống")
    private List<ItemMove> itemsToMove;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemMove {
        @NotBlank(message = "productId không được để trống")
        private String productId;
        private String variantId;
        @Min(value = 1, message = "quantity phải >= 1")
        private int quantity;

        /** Phân biệt dòng cùng product + variant nhưng khác topping */
        private List<String> toppingIds;
    }
}

