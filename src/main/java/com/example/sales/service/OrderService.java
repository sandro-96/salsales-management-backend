package com.example.sales.service;

import com.example.sales.constant.ApiErrorCode;
import com.example.sales.constant.OrderStatus;
import com.example.sales.constant.ShopType;
import com.example.sales.constant.TableStatus;
import com.example.sales.dto.OrderRequest;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.*;
import com.example.sales.repository.OrderRepository;
import com.example.sales.repository.ProductRepository;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;
    private final TableRepository tableRepository;
    private final ProductRepository productRepository;

    public List<Order> getOrdersByUser(User user) {
        Shop shop = getShopOfUser(user);
        return orderRepository.findByShopId(shop.getId());
    }

    @Transactional
    public Order createOrder(User user, OrderRequest request) {
        Shop shop = getShopOfUser(user);

        Order order = new Order();
        order.setShopId(shop.getId());
        order.setTableId(request.getTableId());
        order.setUserId(user.getId());
        order.setItems(request.getItems());
        order.setNote(request.getNote());

        order.setStatus(OrderStatus.PENDING);
        order.setPaid(false);

        // Tính tổng tiền/tổng SL
        double totalAmount = 0;
        double totalPrice = 0;

        for (OrderItem item : request.getItems()) {
            totalAmount += item.getQuantity();
            totalPrice += item.getQuantity() * item.getPrice();
        }

        order.setTotalAmount(totalAmount);
        order.setTotalPrice(totalPrice);
        if (requiresInventory(shop.getType())) {
            for (OrderItem item : order.getItems()) {
                Product product = productRepository.findById(item.getProductId())
                        .filter(p -> p.getShopId().equals(shop.getId()))
                        .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.PRODUCT_NOT_FOUND));

                if (product.getQuantity() < item.getQuantity()) {
                    throw new BusinessException(ApiErrorCode.PRODUCT_OUT_OF_STOCK);
                }

                product.setQuantity(product.getQuantity() - item.getQuantity());
                productRepository.save(product);
            }
        }

        Order createdOrder = orderRepository.save(order);
        releaseTable(createdOrder);
        return createdOrder;
    }

    private boolean requiresInventory(ShopType type) {
        return switch (type) {
            case GROCERY, CONVENIENCE, PHARMACY, RETAIL -> true;
            case RESTAURANT, CAFE, BAR, OTHER -> false;
        };
    }

    public void cancelOrder(User user, String orderId) {
        Shop shop = getShopOfUser(user);
        Order order = getOrderByShop(orderId, shop.getId());

        if (order.isPaid()) {
            throw new BusinessException(ApiErrorCode.ORDER_ALREADY_PAID);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    public Order confirmPayment(User user, String orderId, String paymentId, String paymentMethod) {
        Shop shop = getShopOfUser(user);
        Order order = getOrderByShop(orderId, shop.getId());

        if (order.isPaid()) {
            throw new BusinessException(ApiErrorCode.ORDER_ALREADY_PAID);
        }

        order.setPaid(true);
        order.setPaymentId(paymentId);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(LocalDateTime.now());
        order.setStatus(OrderStatus.COMPLETED); // ✔️ thanh toán xong thì hoàn tất
        Order updatedOrder = orderRepository.save(order);
        releaseTable(updatedOrder);
        return updatedOrder;
    }

    private Shop getShopOfUser(User user) {
        return shopRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new BusinessException(ApiErrorCode.SHOP_NOT_FOUND));
    }

    private Order getOrderByShop(String orderId, String shopId) {
        return orderRepository.findById(orderId)
                .filter(o -> o.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.ORDER_NOT_FOUND));
    }

    public Order updateStatus(User user, String orderId, OrderStatus newStatus) {
        Shop shop = getShopOfUser(user);
        Order order = getOrderByShop(orderId, shop.getId());

        // Nếu là quán ăn → không cho phép chuyển sang SHIPPING
        if (shop.getType() == ShopType.RESTAURANT && newStatus == OrderStatus.SHIPPING) {
            throw new BusinessException(ApiErrorCode.INVALID_STATUS_TRANSITION);
        }

        // Nếu đơn đã huỷ thì không thay đổi
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(ApiErrorCode.ORDER_ALREADY_PAID);
        }

        order.setStatus(newStatus);

        // Nếu COMPLETED → auto paid
        if (newStatus == OrderStatus.COMPLETED && !order.isPaid()) {
            order.setPaid(true);
            order.setPaymentTime(LocalDateTime.now());
            order.setPaymentMethod("Cash");
        }

        return orderRepository.save(order);
    }


    public List<Order> getOrdersByStatus(User user, OrderStatus status) {
        Shop shop = getShopOfUser(user);
        return orderRepository.findByShopId(shop.getId()).stream()
                .filter(order -> order.getStatus() == status)
                .toList();
    }

    private void releaseTable(Order order) {
        if (order.getTableId() != null) {
            tableRepository.findById(order.getTableId()).ifPresent(table -> {
                table.setStatus(TableStatus.AVAILABLE);
                table.setCurrentOrderId(null);
                tableRepository.save(table);
            });
        }
    }

}

