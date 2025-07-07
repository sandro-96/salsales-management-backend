package com.example.sales.service;

import com.example.sales.model.Order;
import com.example.sales.model.User;
import com.example.sales.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public List<Order> getOrdersByUser(User user) {
        return orderRepository.findByUserId(user.getId());
    }

    public Order createOrder(User user, Order order) {
        order.setId(null);
        order.setUserId(user.getId());
        double total = order.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
        order.setTotalPrice(total);
        return orderRepository.save(order);
    }

    public void cancelOrder(User user, String orderId) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getUserId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        order.setStatus("CANCELLED");
        orderRepository.save(order);
    }
}
