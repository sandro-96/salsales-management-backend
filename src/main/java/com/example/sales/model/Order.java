// File: src/main/java/com/example/sales/model/Order.java
package com.example.sales.model;

import com.example.sales.constant.OrderStatus;
import com.example.sales.constant.PaymentStatus;
import com.example.sales.model.base.BaseEntity;
import com.example.sales.model.tax.OrderTaxSnapshot;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString(exclude = "items") // 👈 Quan trọng: không in danh sách item
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("orders")
public class Order extends BaseEntity {
    @Id
    private String id;

    /** Mã đơn hàng hiển thị (VD: DH-20260414-042), sinh khi tạo đơn — khác id nội bộ MongoDB. */
    private String orderCode;

    private String shopId;
    private String branchId;
    private String tableId;
    private String userId;

    private List<OrderItem> items;

    /**
     * Tổng tiền hàng (chưa thuế nếu priceIncludesTax = false)
     */
    private double totalPrice;

    /**
     * Tổng tiền phải trả (luôn = totalPrice + taxTotal)
     */
    private double totalAmount;

    /**
     * Snapshot thuế tại thời điểm tạo order
     */
    private OrderTaxSnapshot taxSnapshot;

    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    private String paymentId;
    private String paymentMethod;
    private LocalDateTime paymentTime;
    private boolean isPaid;

    /** Trạng thái thanh toán (COD chờ thu, đã thu, …). */
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    private String note;

    private String customerId;

    /** Tên khách ghi nhận trên đơn (POS) — tách với customerId (hồ sơ tích điểm). */
    private String guestName;
    /** SĐT khách ghi nhận trên đơn — tách với khách loyalty. */
    private String guestPhone;

    @Builder.Default
    private long pointsEarned = 0;

    @Builder.Default
    private long pointsRedeemed = 0;

    @Builder.Default
    private double pointsDiscount = 0;

    /** Đơn vị vận chuyển (GHN, GHTK, Shopee Xpress, …) — bán lẻ / online */
    private String shippingCarrier;
    /** Hình thức / loại giao (COD ngoài, Shopee, lấy tại quầy, …) */
    private String shippingMethod;
    /** Mã vận đơn */
    private String trackingNumber;
    /** Mã tham chiếu đơn ngoài (VD Shopee) */
    private String externalOrderRef;
}
