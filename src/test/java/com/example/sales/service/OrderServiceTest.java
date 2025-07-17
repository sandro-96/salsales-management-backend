// File: test/java/com/example/sales/service/OrderServiceTest.java
package com.example.sales.service;

import com.example.sales.constant.OrderStatus;
import com.example.sales.model.Order;
import com.example.sales.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AuditLogService auditLogService; // 🟢 THÊM MOCK NÀY

    @Test
    void testCancelOrder() {
        Order mockOrder = Order.builder()
                .id("ord1")
                .shopId("shop1")
                .userId("user1")
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findByIdAndDeletedFalse("ord1"))
                .thenReturn(Optional.of(mockOrder));

        orderService.cancelOrder("user1", "shop1", "ord1");

        assertEquals(OrderStatus.CANCELLED, mockOrder.getStatus());
        verify(orderRepository).save(mockOrder);

        // 🟢 Optional: kiểm tra audit log được gọi
        verify(auditLogService).log(eq("user1"), eq("shop1"), eq("ord1"), eq("ORDER"), eq("CANCELLED"), anyString());
    }
}
