package com.example.sales.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PATCH giao hàng / tham chiếu / khách — cho phép khi đơn đã thanh toán.
 * Trường {@code null} trong JSON = không đổi; chuỗi rỗng = xóa giá trị (nếu có gửi key).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFulfillmentPatchRequest {

    private String note;
    private String customerId;
    private String shippingCarrier;
    private String shippingMethod;
    private String trackingNumber;
    private String externalOrderRef;
}
