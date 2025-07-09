package com.example.sales.service;

import com.example.sales.constant.ApiErrorCode;
import com.example.sales.constant.OrderStatus;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Order;
import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.repository.OrderRepository;
import com.example.sales.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;

    // Lấy danh sách đơn hàng của shop mà user sở hữu
    public List<Order> getOrdersByUser(User user) {
        Shop shop = getShopOfUser(user);
        return orderRepository.findByShopId(shop.getId());
    }

    // Tạo đơn hàng mới
    public Order createOrder(User user, Order order) {
        Shop shop = getShopOfUser(user);

        order.setId(null);
        order.setUserId(user.getId());
        order.setShopId(shop.getId());
        order.setStatus(OrderStatus.PENDING);
        order.setPaid(false); // default chưa thanh toán
        order.setPaymentId(null);
        order.setPaymentTime(null);

        return orderRepository.save(order);
    }

    // Hủy đơn hàng nếu chưa thanh toán và thuộc shop
    public void cancelOrder(User user, String orderId) {
        Shop shop = getShopOfUser(user);
        Order order = getOrderByIdAndShop(orderId, shop.getId());

        if (order.isPaid()) {
            throw new BusinessException(ApiErrorCode.ORDER_ALREADY_PAID);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    // Cập nhật trạng thái đơn hàng (nếu thuộc shop)
    public Order updateOrderStatus(User user, String orderId, OrderStatus newStatus) {
        Shop shop = getShopOfUser(user);
        Order order = getOrderByIdAndShop(orderId, shop.getId());

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(ApiErrorCode.ORDER_ALREADY_PAID);
        }

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    // ====================
    // Private helper methods
    // ====================

    // Lấy shop theo user
    private Shop getShopOfUser(User user) {
        return shopRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new BusinessException(ApiErrorCode.SHOP_NOT_FOUND));
    }

    // Lấy đơn hàng theo ID và shopId để xác thực quyền
    private Order getOrderByIdAndShop(String orderId, String shopId) {
        return orderRepository.findById(orderId)
                .filter(o -> o.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.ORDER_NOT_FOUND));
    }

    public Order confirmPayment(User user, String orderId, String paymentId, String paymentMethod) {
        Shop shop = getShopOfUser(user);
        Order order = getOrderByIdAndShop(orderId, shop.getId());

        if (order.isPaid()) {
            throw new BusinessException(ApiErrorCode.ORDER_ALREADY_PAID);
        }

        order.setPaid(true);
        order.setPaymentId(paymentId);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(LocalDateTime.now());

        return orderRepository.save(order);
    }

}
