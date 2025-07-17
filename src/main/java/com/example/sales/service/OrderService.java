// File: src/main/java/com/example/sales/service/OrderService.java
package com.example.sales.service;

import com.example.sales.constant.*;
import com.example.sales.dto.order.OrderItemResponse;
import com.example.sales.dto.order.OrderRequest;
import com.example.sales.dto.order.OrderResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.*;
import com.example.sales.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final TableRepository tableRepository;
    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;
    private final AuditLogService auditLogService;
    private final ShopRepository shopRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public List<OrderResponse> getOrdersByUser(String userId, String shopId) {
        return orderRepository.findByShopIdAndDeletedFalse(shopId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse createOrder(String userId, String shopId, OrderRequest request) {
        Order order = new Order();
        order.setShopId(shopId);
        order.setTableId(request.getTableId());
        order.setUserId(userId);
        order.setNote(request.getNote());
        order.setStatus(OrderStatus.PENDING);
        order.setPaid(false);

        String branchId = request.getBranchId();
        if (branchId != null && !branchId.isBlank()) {
            order.setBranchId(branchId);
        }

        double[] totals = {0, 0};

        List<OrderItem> orderItems = request.getItems().stream().map(reqItem -> {
            Product product = productRepository.findById(reqItem.getProductId())
                    .filter(p -> p.getShopId().equals(shopId))
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));

            double basePrice = reqItem.getPrice();
            double finalPrice = basePrice;
            String promoId = null;

            Promotion promo = findApplicablePromotion(shopId, branchId, product.getId());
            if (promo != null) {
                promoId = promo.getId();
                if (promo.getDiscountType() == DiscountType.PERCENT) {
                    finalPrice = basePrice * (1 - promo.getDiscountValue() / 100.0);
                } else if (promo.getDiscountType() == DiscountType.AMOUNT) {
                    finalPrice = Math.max(0, basePrice - promo.getDiscountValue());
                }
            }

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setQuantity(reqItem.getQuantity());
            item.setPrice(basePrice);
            item.setPriceAfterDiscount(finalPrice);
            item.setAppliedPromotionId(promoId);

            totals[0] += reqItem.getQuantity();
            totals[1] += reqItem.getQuantity() * finalPrice;

            return item;
        }).toList();

        order.setItems(orderItems);
        order.setTotalAmount(totals[0]);
        order.setTotalPrice(totals[1]);

        Order created = orderRepository.save(order);
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.SHOP_NOT_FOUND));

        if (shop.getType().isTrackInventory()) {
            for (OrderItem item : created.getItems()) {
                Product product = productRepository.findByIdAndDeletedFalse(item.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));

                adjustInventory(product, shopId, created.getBranchId(), InventoryType.EXPORT, item.getQuantity(),
                        "Xuất kho theo đơn hàng " + created.getId(), created.getId());
            }
        }
        releaseTable(created);
        return toResponse(created);
    }

    public void cancelOrder(String userId, String shopId, String orderId) {
        Order order = getOrderByShop(orderId, shopId);

        if (order.isPaid()) {
            throw new BusinessException(ApiCode.ORDER_ALREADY_PAID);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        auditLogService.log(userId, shopId, order.getId(), "ORDER", "CANCELLED", "Huỷ đơn hàng");
    }

    public OrderResponse confirmPayment(String userId, String shopId, String orderId, String paymentId, String paymentMethod) {
        Order order = getOrderByShop(orderId, shopId);

        if (order.isPaid()) {
            throw new BusinessException(ApiCode.ORDER_ALREADY_PAID);
        }

        order.setPaid(true);
        order.setPaymentId(paymentId);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(LocalDateTime.now());
        order.setStatus(OrderStatus.COMPLETED);

        Order updated = orderRepository.save(order);
        releaseTable(updated);
        auditLogService.log(userId, shopId, order.getId(), "ORDER", "PAYMENT_CONFIRMED",
                "Xác nhận thanh toán đơn hàng với ID: %s".formatted(orderId));
        return toResponse(updated);
    }

    public OrderResponse updateStatus(String userId, String shopId, String orderId, OrderStatus newStatus) {
        Order order = getOrderByShop(orderId, shopId);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(ApiCode.ORDER_ALREADY_PAID);
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);

        if (newStatus == OrderStatus.COMPLETED && !order.isPaid()) {
            order.setPaid(true);
            order.setPaymentTime(LocalDateTime.now());
            order.setPaymentMethod("Cash");
        }

        Order updated = orderRepository.save(order);
        if (!oldStatus.equals(newStatus)) {
            auditLogService.log(userId, shopId, order.getId(), "ORDER", "STATUS_UPDATED",
                    "Cập nhật trạng thái từ %s → %s".formatted(oldStatus, newStatus));
        }

        return toResponse(updated);
    }

    public List<OrderResponse> getOrdersByStatus(String userId, String shopId, OrderStatus status, String branchId) {
        return orderRepository.findByShopIdAndBranchIdAndStatusAndDeletedFalse(shopId, branchId, status)
                .stream().map(this::toResponse).toList();
    }

    private Order getOrderByShop(String orderId, String shopId) {
        return orderRepository.findByIdAndDeletedFalse(orderId)
                .filter(o -> o.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.ORDER_NOT_FOUND));
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

    private Promotion findApplicablePromotion(String shopId, String branchId, String productId) {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findByShopIdAndDeletedFalse(shopId).stream()
                .filter(Promotion::isActive)
                .filter(p -> p.getBranchId() == null || p.getBranchId().equals(branchId))
                .filter(p -> !p.getStartDate().isAfter(now) && !p.getEndDate().isBefore(now))
                .filter(p -> p.getApplicableProductIds() == null
                        || p.getApplicableProductIds().isEmpty()
                        || p.getApplicableProductIds().contains(productId))
                .findFirst()
                .orElse(null);
    }

    private void adjustInventory(Product product, String shopId, String branchId, InventoryType type, int qty,
                                 String note, String referenceId) {

        int change = switch (type) {
            case EXPORT -> -qty;
            case IMPORT, ADJUSTMENT -> qty;
        };

        int newQty = product.getQuantity() + change;
        if (newQty < 0) {
            throw new BusinessException(ApiCode.PRODUCT_OUT_OF_STOCK);
        }

        product.setQuantity(newQty);
        productRepository.save(product);

        InventoryTransaction tx = InventoryTransaction.builder()
                .shopId(shopId)
                .branchId(branchId)
                .productId(product.getId())
                .type(type)
                .quantity(change)
                .note(note)
                .referenceId(referenceId)
                .build();

        inventoryTransactionRepository.save(tx);
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .tableId(order.getTableId())
                .note(order.getNote())
                .status(order.getStatus())
                .paid(order.isPaid())
                .paymentMethod(order.getPaymentMethod())
                .paymentId(order.getPaymentId())
                .paymentTime(order.getPaymentTime())
                .totalAmount(order.getTotalAmount())
                .totalPrice(order.getTotalPrice())
                .items(order.getItems().stream().map(this::toItemResponse).toList())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .priceAfterDiscount(item.getPriceAfterDiscount())
                .appliedPromotionId(item.getAppliedPromotionId())
                .build();
    }
}
