// File: src/main/java/com/example/sales/service/payment/PaymentInitiation.java
package com.example.sales.service.payment;

import com.example.sales.constant.PaymentGatewayType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentInitiation {
    private PaymentGatewayType gateway;
    /** URL redirect tới trang thanh toán của gateway. Stub: URL mock nội bộ. */
    private String paymentUrl;
    /** Transaction ID do gateway sinh ra — dùng để verify webhook sau. */
    private String transactionId;
    private long amountVnd;
}
