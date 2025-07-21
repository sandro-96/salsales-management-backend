// File: src/test/java/com/example/sales/service/OrderServiceTest.java
package com.example.sales.service;

import com.example.sales.constant.*;
import com.example.sales.dto.order.OrderItemRequest;
import com.example.sales.dto.order.OrderRequest;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.*;
import com.example.sales.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PromotionRepository promotionRepository;
    @Mock private ShopRepository shopRepository;
    @Mock private TableRepository tableRepository;
    @Mock private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private ShopUserService shopUserService;

    @InjectMocks private OrderService orderService;

    private final String userId = "user123";
    private final String shopId = "shop123";
    private final String branchId = "branch123";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createOrder_shouldCreateOrderAndAdjustInventory() {
        Product product = Product.builder()
                .id("prod1")
                .name("Coca")
                .quantity(100)
                .shopId(shopId)
                .build();

        Shop shop = new Shop();
        shop.setId(shopId);
        shop.setType(ShopType.RETAIL);

        OrderItemRequest itemRequest = new OrderItemRequest("prod1", 2, 10000);
        OrderRequest request = new OrderRequest();
        request.setItems(List.of(itemRequest));
        request.setBranchId(branchId);

        when(productRepository.findById("prod1")).thenReturn(Optional.of(product));
        when(shopRepository.findByIdAndDeletedFalse(shopId)).thenReturn(Optional.of(shop));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = orderService.createOrder(userId, shopId, request);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getTotalAmount()).isEqualTo(2);
        assertThat(response.getTotalPrice()).isEqualTo(20000);

        verify(productRepository).save(any());
        verify(inventoryTransactionRepository).save(any());
    }

    @Test
    void createOrder_shouldThrowIfProductNotFound() {
        OrderRequest request = new OrderRequest();
        request.setItems(List.of(new OrderItemRequest("missingProd", 1, 5000)));

        when(productRepository.findById("missingProd")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(userId, shopId, request))
                .isInstanceOf(Exception.class);
    }

    @Test
    void cancelOrder_shouldSetStatusCancelled() {
        Order order = new Order();
        order.setId("order123");
        order.setShopId(shopId);
        order.setUserId(userId);
        order.setPaid(false);

        when(orderRepository.findByIdAndDeletedFalse("order123")).thenReturn(Optional.of(order));

        orderService.cancelOrder(userId, shopId, "order123");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrder_shouldFailIfAlreadyPaid() {
        Order order = new Order();
        order.setId("order123");
        order.setPaid(true);
        order.setShopId(shopId);
        order.setUserId(userId);

        when(orderRepository.findByIdAndDeletedFalse("order123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(userId, shopId, "order123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ORDER_ALREADY_PAID");
    }
}

