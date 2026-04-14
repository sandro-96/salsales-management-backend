// File: src/main/java/com/example/sales/dto/order/OrderResponse.java
package com.example.sales.dto.order;

import com.example.sales.constant.OrderStatus;
import com.example.sales.constant.PaymentStatus;
import com.example.sales.model.tax.OrderTaxSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String id;
    /** Mã đơn hàng (hiển thị cho khách / in bill); đơn cũ có thể suy từ id nếu chưa lưu mã. */
    private String orderCode;
    private String shopId;          // ID của cửa hàng
    private String branchId;        // ID của chi nhánh mà đơn hàng thuộc về
    private String tableId;         // ID của bàn (nếu có)
    private String userId;          // ID của người dùng tạo đơn hàng
    private String note;
    private OrderStatus status;     // Trạng thái đơn hàng (PENDING, COMPLETED, CANCELLED, etc.)
    private boolean paid;           // Đã thanh toán chưa
    private PaymentStatus paymentStatus;
    private String paymentMethod;   // Phương thức thanh toán
    private String paymentId;       // ID giao dịch thanh toán (nếu có)
    private LocalDateTime paymentTime; // Thời gian thanh toán
    private double totalAmount;     // Tổng số lượng sản phẩm (ví dụ: tổng số lượng items)
    private double totalPrice;      // Tổng giá trị đơn hàng sau chiết khấu
    private List<OrderItemResponse> items; // Danh sách các mục trong đơn hàng
    private OrderTaxSnapshot taxSnapshot; // Thông tin thuế tại thời điểm tạo đơn hàng

    private String customerId;
    /** Tên khách (suy ra khi trả API, không lưu trên Order). */
    private String customerName;
    private String customerPhone;
    private long pointsEarned;
    private long pointsRedeemed;
    private double pointsDiscount;

    private String shippingCarrier;
    private String shippingMethod;
    private String trackingNumber;
    private String externalOrderRef;

    /** Thời điểm tạo đơn (audit). */
    private LocalDateTime createdAt;
}