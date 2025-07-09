package com.example.sales.model;

import com.example.sales.constant.OrderStatus;
import com.example.sales.model.base.BaseEntity;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document("orders")
@Data
public class Order extends BaseEntity {
    @Id
    private String id;

    private String userId;               // Người đặt đơn
    private List<OrderItem> items;      // Danh sách sản phẩm trong đơn
    private double totalPrice;          // Tổng tiền
    private double totalAmount;         // Tổng số lượng

    private String shopId;              // ID cửa hàng
    private String tableId;             // Bàn phục vụ (nếu có)

    private OrderStatus status = OrderStatus.PENDING;

    // ---------- Các trường thanh toán ----------
    private String paymentId;           // Mã giao dịch thanh toán (VNPay, Momo, v.v.)
    private LocalDateTime paymentTime;  // Thời gian thanh toán
    private boolean isPaid = false;     // Trạng thái đã thanh toán hay chưa
    private String paymentMethod;       // Phương thức thanh toán (e.g., "Momo", "VNPay", "Cash")

    private String note;                // Ghi chú đơn hàng (nếu có)
}
