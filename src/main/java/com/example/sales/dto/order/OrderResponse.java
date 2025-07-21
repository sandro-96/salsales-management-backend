// File: src/main/java/com/example/sales/dto/order/OrderResponse.java
package com.example.sales.dto.order;

import com.example.sales.constant.OrderStatus;
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
    private String shopId;          // ID của cửa hàng
    private String branchId;        // ID của chi nhánh mà đơn hàng thuộc về
    private String tableId;         // ID của bàn (nếu có)
    private String userId;          // ID của người dùng tạo đơn hàng
    private String note;
    private OrderStatus status;     // Trạng thái đơn hàng (PENDING, COMPLETED, CANCELLED, etc.)
    private boolean paid;           // Đã thanh toán chưa
    private String paymentMethod;   // Phương thức thanh toán
    private String paymentId;       // ID giao dịch thanh toán (nếu có)
    private LocalDateTime paymentTime; // Thời gian thanh toán
    private double totalAmount;     // Tổng số lượng sản phẩm (ví dụ: tổng số lượng items)
    private double totalPrice;      // Tổng giá trị đơn hàng sau chiết khấu
    private List<OrderItemResponse> items; // Danh sách các mục trong đơn hàng

    // OrderItemResponse đã được định nghĩa trong OrderService trước đó, nhưng ta có thể định nghĩa lại tại đây cho rõ ràng.
    // Nếu bạn muốn giữ nó là inner class trong OrderService.java (như trong OrderService.java bạn đã cung cấp),
    // thì không cần định nghĩa lại ở đây.
    // Tuy nhiên, việc có một DTO riêng biệt cho OrderItemResponse thường tốt hơn để tái sử dụng.
    // Tôi sẽ định nghĩa nó như một inner static class ở đây để đảm bảo nó tồn tại,
    // hoặc bạn có thể tạo một file OrderItemResponse.java riêng biệt.
    // Để giữ cho tổ chức gọn gàng, tôi sẽ tạo một file riêng cho OrderItemResponse.java
}