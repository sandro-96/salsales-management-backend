// File: src/main/java/com/example/sales/service/OrderService.java
package com.example.sales.service;

import com.example.sales.cache.OrderCache;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.DiscountType;
import com.example.sales.constant.OrderStatus;
import com.example.sales.constant.TableStatus;
import com.example.sales.dto.order.OrderItemResponse;
import com.example.sales.dto.order.OrderRequest;
import com.example.sales.dto.order.OrderResponse;
import com.example.sales.dto.order.OrderUpdateRequest;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.*;
import com.example.sales.repository.*;
import com.example.sales.service.tax.OrderTaxApplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j // Khởi tạo Log
public class OrderService extends BaseService {

    private final OrderRepository orderRepository;
    private final TableRepository tableRepository;
    private final ProductRepository productRepository;
    private final BranchProductRepository branchProductRepository;
    private final PromotionRepository promotionRepository;
    private final AuditLogService auditLogService;
    private final ShopRepository shopRepository;
    private final InventoryService inventoryService;
    private final OrderCache orderCache;
    private final OrderTaxApplier orderTaxApplier;

    @Transactional
    public OrderResponse createOrder(String userId, String branchId, String shopId, OrderRequest request) {
        Order order = new Order();
        order.setShopId(shopId);
        order.setTableId(request.getTableId());
        order.setUserId(userId);
        order.setNote(request.getNote());
        order.setStatus(OrderStatus.PENDING);
        order.setPaid(false);

        if (branchId == null || branchId.isBlank()) {
            log.error("Branch ID không được để trống");
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        order.setBranchId(branchId);

        double[] totals = {0, 0};

        // Lấy thông tin shop để kiểm tra loại hình kinh doanh (có quản lý tồn kho không)
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.SHOP_NOT_FOUND));


        List<OrderItem> orderItems = request.getItems().stream().map(reqItem -> {
            // Lấy Product master
            Product masterProduct = productRepository.findByIdAndDeletedFalse(reqItem.getProductId())
                    .filter(p -> p.getShopId().equals(shopId))
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));

            // Lấy BranchProduct cho chi nhánh và sản phẩm cụ thể
            BranchProduct branchProduct = branchProductRepository
                    .findByProductIdAndBranchIdAndDeletedFalse(masterProduct.getId(), branchId)
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));


            double basePrice = branchProduct.getPrice();
            double finalPrice = basePrice;
            String promoId = null;

            Promotion promo = findApplicablePromotion(shopId, branchId, masterProduct.getId()); // Áp dụng promo dựa trên masterProduct ID
            if (promo != null) {
                promoId = promo.getId();
                if (promo.getDiscountType() == DiscountType.PERCENT) {
                    finalPrice = basePrice * (1 - promo.getDiscountValue() / 100.0);
                } else if (promo.getDiscountType() == DiscountType.AMOUNT) {
                    finalPrice = Math.max(0, basePrice - promo.getDiscountValue());
                }
            }

            OrderItem item = OrderItem.builder()
                    .productId(masterProduct.getId()) // Lưu master product ID
                    .branchProductId(branchProduct.getId()) // Lưu BranchProduct ID
                    .productName(masterProduct.getName())
                    .quantity(reqItem.getQuantity())
                    .price(basePrice)
                    .priceAfterDiscount(finalPrice)
                    .appliedPromotionId(promoId)
                    .build();

            totals[0] += reqItem.getQuantity();
            totals[1] += reqItem.getQuantity() * finalPrice;

            return item;
        }).toList();

        order.setItems(orderItems);
        order.setTotalPrice(totals[1]);
        orderTaxApplier.applyTax(order);

        Order created = orderRepository.save(order);

        // Điều chỉnh tồn kho sau khi tạo đơn hàng
        if (shop.getType().isTrackInventory()) {
            for (OrderItem item : created.getItems()) {
                // Sử dụng BranchProduct ID để điều chỉnh tồn kho
                inventoryService.exportProductQuantity(
                        userId, shopId, created.getBranchId(), item.getBranchProductId(),
                        item.getQuantity(), "Xuất kho theo đơn hàng " + created.getId(), created.getId());
            }
        }
        occupyTable(created);
        auditLogService.log(userId, shopId, created.getId(), "ORDER", "CREATED", "Tạo đơn hàng mới");
        return toResponse(created);
    }

    public void cancelOrder(String userId, String shopId, String orderId) {
        Order order = orderCache.getOrderByShop(orderId, shopId);

        if (order.isPaid()) {
            throw new BusinessException(ApiCode.ORDER_ALREADY_PAID);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Hoàn kho khi hủy đơn hàng nếu shop có quản lý tồn kho
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.SHOP_NOT_FOUND));
        if (shop.getType().isTrackInventory()) {
            for (OrderItem item : order.getItems()) {
                inventoryService.importProductQuantity(
                        userId, shopId, order.getBranchId(), item.getBranchProductId(),
                        item.getQuantity(), "Hoàn kho khi hủy đơn hàng " + orderId);
            }
        }

        auditLogService.log(userId, shopId, order.getId(), "ORDER", "CANCELLED", "Huỷ đơn hàng");
    }

    public OrderResponse confirmPayment(String userId, String shopId, String orderId, String paymentId, String paymentMethod) {
        Order order = orderCache.getOrderByShop(orderId, shopId);

        if (order.isPaid()) {
            throw new BusinessException(ApiCode.ORDER_ALREADY_PAID);
        }

        order.setPaid(true);
        order.setPaymentId(paymentId);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(LocalDateTime.now());
        order.setStatus(OrderStatus.COMPLETED);

        Order updated = orderRepository.save(order);
        releaseTable(updated); // Giải phóng bàn khi đơn hàng hoàn thành/thanh toán
        auditLogService.log(userId, shopId, order.getId(), "ORDER", "PAYMENT_CONFIRMED",
                "Xác nhận thanh toán đơn hàng với ID: %s".formatted(orderId));
        return toResponse(updated);
    }

    public OrderResponse updateStatus(String userId, String shopId, String orderId, OrderStatus newStatus) {
        Order order = orderCache.getOrderByShop(orderId, shopId);

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            log.error("Không thể cập nhật trạng thái đơn hàng đã hủy hoặc đã hoàn thành");
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        if (newStatus == OrderStatus.CANCELLED && order.isPaid()) {
            log.error("Không thể hủy đơn hàng đã thanh toán");
            throw new BusinessException(ApiCode.ORDER_ALREADY_PAID);
        }


        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);

        if (newStatus == OrderStatus.COMPLETED && !order.isPaid()) {
            // Nếu đơn hàng chuyển sang COMPLETED mà chưa thanh toán, coi như thanh toán bằng tiền mặt
            order.setPaid(true);
            order.setPaymentTime(LocalDateTime.now());
            order.setPaymentMethod("Cash");
            releaseTable(order); // Giải phóng bàn
        }

        Order updated = orderRepository.save(order);
        if (!oldStatus.equals(newStatus)) {
            auditLogService.log(userId, shopId, order.getId(), "ORDER", "STATUS_UPDATED",
                    "Cập nhật trạng thái từ %s → %s".formatted(oldStatus, newStatus));
        }

        return toResponse(updated);
    }

    private void occupyTable(Order order) {
        if (order.getTableId() != null && !order.getTableId().isBlank()) { // Kiểm tra null và blank
            tableRepository.findByIdAndDeletedFalse(order.getTableId()).ifPresent(table -> { // Tìm bàn không bị xóa
                table.setStatus(TableStatus.OCCUPIED);
                table.setCurrentOrderId(order.getId());
                tableRepository.save(table);
            });
        }
    }

    private void releaseTable(Order order) {
        if (order.getTableId() != null && !order.getTableId().isBlank()) {
            tableRepository.findByIdAndDeletedFalse(order.getTableId()).ifPresent(table -> {
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
                .filter(p -> p.getBranchId() == null || p.getBranchId().equals(branchId)) // Khuyến mãi có thể áp dụng cho toàn bộ shop (branchId = null) hoặc riêng cho 1 branch
                .filter(p -> !p.getStartDate().isAfter(now) && !p.getEndDate().isBefore(now))
                .filter(p -> p.getApplicableProductIds() == null
                        || p.getApplicableProductIds().isEmpty()
                        || p.getApplicableProductIds().contains(productId)) // Áp dụng cho masterProduct ID
                .findFirst()
                .orElse(null);
    }

    public Page<OrderResponse> getShopOrders(String shopId, Pageable pageable) {
        return orderRepository.findByShopIdOrderByCreatedAtDesc(shopId, pageable)
                .map(this::toResponse);
    }

    public Page<OrderResponse> getOrdersByStatus(String shopId, OrderStatus status, String branchId, Pageable pageable) {
        return orderRepository.findByShopIdAndBranchIdAndStatusAndDeletedFalse(shopId, branchId, status, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public OrderResponse updateOrder(String userId, String shopId, String orderId, OrderUpdateRequest request) {
        Order order = orderCache.getOrderByShop(orderId, shopId);

        if (order.isPaid()) {
            throw new BusinessException(ApiCode.ORDER_ALREADY_PAID);
        }

        // Gỡ bàn cũ nếu đổi bàn
        if (request.getTableId() != null && !request.getTableId().equals(order.getTableId())) {
            releaseTable(order);
            String oldTableId = order.getTableId();
            order.setTableId(request.getTableId());
            occupyTable(order);
            auditLogService.log(userId, shopId, orderId, "ORDER", "TABLE_CHANGED",
                    "Đổi bàn cho đơn hàng từ %s sang %s".formatted(oldTableId, request.getTableId()));
        }

        if (request.getNote() != null) {
            order.setNote(request.getNote());
        }

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.SHOP_NOT_FOUND));

            // 🔁 1. Hoàn tác lại tồn kho theo đơn hàng cũ (nếu shop có quản lý tồn kho)
            if (shop.getType().isTrackInventory()) {
                for (OrderItem oldItem : order.getItems()) {
                    inventoryService.importProductQuantity(
                            userId, shopId, order.getBranchId(), oldItem.getBranchProductId(), // Sử dụng BranchProduct ID
                            oldItem.getQuantity(), "Hoàn kho khi cập nhật đơn hàng " + orderId);
                }
            }

            // 🔁 2. Áp dụng lại tồn kho cho danh sách mới và tính toán lại tổng tiền
            double[] totals = {0, 0};

            List<OrderItem> updatedItems = request.getItems().stream().map(reqItem -> {
                // Lấy Product master
                Product masterProduct = productRepository.findByIdAndDeletedFalse(reqItem.getProductId())
                        .filter(p -> p.getShopId().equals(shopId))
                        .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));

                // Lấy BranchProduct cho chi nhánh và sản phẩm cụ thể
                BranchProduct branchProduct = branchProductRepository
                        .findByProductIdAndBranchIdAndDeletedFalse(masterProduct.getId(), order.getBranchId()) // Lấy branchId từ order
                        .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));

                double basePrice = branchProduct.getPrice();
                double finalPrice = basePrice;
                String promoId = null;

                Promotion promo = findApplicablePromotion(shopId, order.getBranchId(), masterProduct.getId());
                if (promo != null) {
                    promoId = promo.getId();
                    if (promo.getDiscountType() == DiscountType.PERCENT) {
                        finalPrice = basePrice * (1 - promo.getDiscountValue() / 100.0);
                    } else if (promo.getDiscountType() == DiscountType.AMOUNT) {
                        finalPrice = Math.max(0, basePrice - promo.getDiscountValue());
                    }
                }

                OrderItem item = OrderItem.builder()
                        .productId(masterProduct.getId())
                        .branchProductId(branchProduct.getId()) // Lưu BranchProduct ID
                        .productName(masterProduct.getName())
                        .quantity(reqItem.getQuantity())
                        .price(basePrice)
                        .priceAfterDiscount(finalPrice)
                        .appliedPromotionId(promoId)
                        .build();

                // Trừ kho mới (nếu shop có quản lý tồn kho)
                if (shop.getType().isTrackInventory()) {
                    inventoryService.exportProductQuantity(
                            userId, shopId, order.getBranchId(), item.getBranchProductId(), // Sử dụng BranchProduct ID
                            item.getQuantity(), "Xuất kho khi cập nhật đơn hàng " + orderId, orderId);
                }

                totals[0] += reqItem.getQuantity();
                totals[1] += reqItem.getQuantity() * finalPrice;
                return item;
            }).toList();

            order.setItems(updatedItems);
            order.setTotalPrice(totals[1]);
            orderTaxApplier.applyTax(order);
        }

        Order updated = orderRepository.save(order);
        auditLogService.log(userId, shopId, orderId, "ORDER", "UPDATED", "Cập nhật đơn hàng");
        return toResponse(updated);
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .tableId(order.getTableId())
                .branchId(order.getBranchId()) // Thêm branchId vào response
                .note(order.getNote())
                .status(order.getStatus())
                .paid(order.isPaid())
                .paymentMethod(order.getPaymentMethod())
                .paymentId(order.getPaymentId())
                .paymentTime(order.getPaymentTime())
                .totalAmount(order.getTotalAmount())
                .totalPrice(order.getTotalPrice())
                .items(order.getItems().stream().map(this::toItemResponse).toList())
                .taxSnapshot(order.getTaxSnapshot())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .productId(item.getProductId())
                .branchProductId(item.getBranchProductId()) // Thêm branchProductId vào response
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .priceAfterDiscount(item.getPriceAfterDiscount())
                .appliedPromotionId(item.getAppliedPromotionId())
                .build();
    }
}