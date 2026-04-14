// File: src/main/java/com/example/sales/service/OrderService.java
package com.example.sales.service;

import com.example.sales.cache.OrderCache;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.AppConstants;
import com.example.sales.constant.DiscountType;
import com.example.sales.constant.OrderStatus;
import com.example.sales.constant.PaymentStatus;
import com.example.sales.constant.TableStatus;
import com.example.sales.dto.order.OrderFulfillmentPatchRequest;
import com.example.sales.dto.order.OrderItemResponse;
import com.example.sales.dto.order.OrderRequest;
import com.example.sales.dto.order.OrderResponse;
import com.example.sales.dto.order.OrderUpdateRequest;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.*;
import com.example.sales.repository.*;
import com.example.sales.service.tax.OrderTaxApplier;
import com.example.sales.util.OrderDisplayUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final LoyaltyService loyaltyService;
    private final CustomerRepository customerRepository;
    private final SequenceService sequenceService;

    private static final DateTimeFormatter ORDER_CODE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    @Transactional
    public OrderResponse createOrder(String userId, String branchId, String shopId, OrderRequest request) {
        Order order = new Order();
        order.setShopId(shopId);
        order.setTableId(request.getTableId());
        order.setUserId(userId);
        order.setNote(request.getNote());
        order.setStatus(OrderStatus.PENDING);
        order.setPaid(false);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        if (StringUtils.hasText(request.getCheckoutPaymentMethod())
                && "ShipCOD".equalsIgnoreCase(request.getCheckoutPaymentMethod().trim())) {
            order.setPaymentMethod("Ship COD");
            order.setPaymentStatus(PaymentStatus.PENDING_COLLECTION);
            order.setPaid(false);
            order.setStatus(OrderStatus.CONFIRMED);
        }
        order.setCustomerId(request.getCustomerId());
        if (StringUtils.hasText(request.getShippingCarrier())) {
            order.setShippingCarrier(request.getShippingCarrier().trim());
        }
        if (StringUtils.hasText(request.getShippingMethod())) {
            order.setShippingMethod(request.getShippingMethod().trim());
        }
        if (StringUtils.hasText(request.getTrackingNumber())) {
            order.setTrackingNumber(request.getTrackingNumber().trim());
        }
        if (StringUtils.hasText(request.getExternalOrderRef())) {
            order.setExternalOrderRef(request.getExternalOrderRef().trim());
        }

        if (branchId == null || branchId.isBlank()) {
            log.error("Branch ID không được để trống");
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        order.setBranchId(branchId);
        order.setOrderCode(generateOrderCode(shopId));

        double[] totals = {0, 0};

        List<OrderItem> orderItems = request.getItems().stream().map(reqItem -> {
            // Lấy Product master
            Product masterProduct = productRepository.findByIdAndDeletedFalse(reqItem.getProductId())
                    .filter(p -> p.getShopId().equals(shopId))
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));

            // Lấy BranchProduct cho chi nhánh và sản phẩm cụ thể
            BranchProduct branchProduct = branchProductRepository
                    .findByProductIdAndBranchIdAndDeletedFalse(masterProduct.getId(), branchId)
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));

            OrderItem item = buildOrderItemLine(
                    masterProduct, branchProduct, reqItem.getVariantId(), reqItem.getQuantity(), shopId, branchId);

            totals[0] += reqItem.getQuantity();
            totals[1] += reqItem.getQuantity() * item.getPriceAfterDiscount();

            return item;
        }).toList();

        order.setItems(orderItems);
        order.setTotalPrice(totals[1]);
        orderTaxApplier.applyTax(order);

        Order created = orderRepository.save(order);

        // Áp dụng đổi điểm nếu có (sau khi có orderId)
        if (request.getCustomerId() != null && request.getPointsToRedeem() > 0) {
            long discount = loyaltyService.redeemPoints(
                    shopId, branchId, request.getCustomerId(),
                    request.getPointsToRedeem(), created.getId(), userId);
            created.setPointsRedeemed(request.getPointsToRedeem());
            created.setPointsDiscount(discount);
            created.setTotalPrice(Math.max(0, totals[1] - discount));
            orderTaxApplier.applyTax(created);
            created = orderRepository.save(created);
        }

        // Điều chỉnh tồn kho sau khi tạo đơn hàng
        for (OrderItem item : created.getItems()) {
            if (item.isTrackInventory()) {
                // Sử dụng BranchProduct ID để điều chỉnh tồn kho
                inventoryService.exportProductQuantity(
                        userId, shopId, created.getBranchId(), item.getBranchProductId(),
                        item.getVariantId(),
                        item.getQuantity(),
                        "Xuất kho theo đơn hàng " + OrderDisplayUtils.displayOrderCode(created),
                        created.getId());
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
        orderCache.evict(orderId, shopId);

        // Hoàn kho khi hủy đơn hàng nếu shop có quản lý tồn kho
        for (OrderItem item : order.getItems()) {
            if (item.isTrackInventory()) {
                inventoryService.importProductQuantity(
                        userId, shopId, order.getBranchId(), item.getBranchProductId(),
                        item.getVariantId(),
                        item.getQuantity(),
                        "Hoàn kho khi hủy đơn hàng " + OrderDisplayUtils.displayOrderCode(order));
            }
        }

        // Hoàn điểm khi hủy đơn hàng
        if (order.getCustomerId() != null && !order.getCustomerId().isBlank()) {
            try {
                loyaltyService.refundPoints(shopId, order.getBranchId(),
                        order.getCustomerId(), orderId, userId);
            } catch (Exception e) {
                log.warn("Không thể hoàn điểm cho đơn hàng {}: {}", orderId, e.getMessage());
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
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentId(paymentId);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(LocalDateTime.now());
        order.setStatus(OrderStatus.COMPLETED);

        Order updated = orderRepository.save(order);
        releaseTable(updated); // Giải phóng bàn khi đơn hàng hoàn thành/thanh toán

        // Tích điểm cho khách hàng
        if (updated.getCustomerId() != null && !updated.getCustomerId().isBlank()) {
            try {
                loyaltyService.earnPoints(shopId, updated.getBranchId(),
                        updated.getCustomerId(), updated.getTotalAmount(),
                        updated.getId(), userId);
                // Cập nhật lại điểm đã tích vào order
                long earned = (long) (updated.getTotalAmount() / 10_000);
                updated.setPointsEarned(earned);
                orderRepository.save(updated);
            } catch (Exception e) {
                log.warn("Không thể tích điểm cho đơn hàng {}: {}", orderId, e.getMessage());
            }
        }

        auditLogService.log(userId, shopId, order.getId(), "ORDER", "PAYMENT_CONFIRMED",
                "Xác nhận thanh toán đơn hàng với ID: %s".formatted(orderId));
        orderCache.evict(orderId, shopId);
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
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaymentTime(LocalDateTime.now());
            order.setPaymentMethod("Cash");
            releaseTable(order); // Giải phóng bàn
        }

        Order updated = orderRepository.save(order);
        orderCache.evict(orderId, shopId);
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

    private boolean hasTrackedVariants(BranchProduct bp) {
        return bp.getVariants() != null && !bp.getVariants().isEmpty();
    }

    private BranchProductVariant requireBranchVariant(BranchProduct bp, String variantId) {
        if (!StringUtils.hasText(variantId)) {
            throw new BusinessException(ApiCode.ORDER_LINE_VARIANT_REQUIRED);
        }
        return bp.getVariants().stream()
                .filter(v -> variantId.equals(v.getVariantId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_VARIANT_NOT_FOUND));
    }

    private void validateOrderLineVariant(BranchProduct bp, String variantId) {
        if (hasTrackedVariants(bp)) {
            requireBranchVariant(bp, variantId);
        } else if (StringUtils.hasText(variantId)) {
            throw new BusinessException(ApiCode.ORDER_LINE_VARIANT_NOT_ALLOWED);
        }
    }

    private double resolveLineBasePrice(BranchProduct bp, String variantId) {
        if (!hasTrackedVariants(bp)) {
            return bp.getPrice();
        }
        BranchProductVariant v = requireBranchVariant(bp, variantId);
        return v.getPrice() > 0 ? v.getPrice() : bp.getPrice();
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

    public Page<OrderResponse> getShopOrders(String shopId, String branchId, Pageable pageable) {
        if (StringUtils.hasText(branchId)) {
            return orderRepository
                    .findByShopIdAndBranchIdAndDeletedFalseOrderByCreatedAtDesc(shopId, branchId, pageable)
                    .map(this::toResponse);
        }
        return orderRepository.findByShopIdAndDeletedFalseOrderByCreatedAtDesc(shopId, pageable)
                .map(this::toResponse);
    }

    public Page<OrderResponse> getOrdersByStatus(String shopId, OrderStatus status, String branchId, Pageable pageable) {
        if (StringUtils.hasText(branchId)) {
            return orderRepository
                    .findByShopIdAndBranchIdAndStatusAndDeletedFalse(shopId, branchId, status, pageable)
                    .map(this::toResponse);
        }
        return orderRepository.findByShopIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(shopId, status, pageable)
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
            // 🔁 1. Hoàn tác lại tồn kho theo đơn hàng cũ (nếu shop có quản lý tồn kho)
            for (OrderItem oldItem : order.getItems()) {
                if (oldItem.isTrackInventory()) {
                    inventoryService.importProductQuantity(
                        userId, shopId, order.getBranchId(), oldItem.getBranchProductId(), // Sử dụng BranchProduct ID
                        oldItem.getVariantId(),
                        oldItem.getQuantity(),
                        "Hoàn kho khi cập nhật đơn hàng " + OrderDisplayUtils.displayOrderCode(order));
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

                OrderItem item = buildOrderItemLine(
                        masterProduct,
                        branchProduct,
                        reqItem.getVariantId(),
                        reqItem.getQuantity(),
                        shopId,
                        order.getBranchId());

                // Trừ kho mới (nếu shop có quản lý tồn kho)
                if (item.isTrackInventory()) {
                    inventoryService.exportProductQuantity(
                            userId, shopId, order.getBranchId(), item.getBranchProductId(), // Sử dụng BranchProduct ID
                            item.getVariantId(),
                            item.getQuantity(),
                            "Xuất kho khi cập nhật đơn hàng " + OrderDisplayUtils.displayOrderCode(order),
                            orderId);
                }
                totals[0] += reqItem.getQuantity();
                totals[1] += reqItem.getQuantity() * item.getPriceAfterDiscount();
                return item;
            }).toList();    

            order.setItems(updatedItems);
            order.setTotalPrice(totals[1]);
            orderTaxApplier.applyTax(order);
        }

        Order updated = orderRepository.save(order);
        orderCache.evict(orderId, shopId);
        auditLogService.log(userId, shopId, orderId, "ORDER", "UPDATED", "Cập nhật đơn hàng");
        return toResponse(updated);
    }

    /**
     * Cập nhật ghi chú, khách hàng, thông tin vận chuyển / tham chiếu — cho phép cả đơn đã thanh toán.
     */
    @Transactional
    public OrderResponse patchOrderFulfillment(
            String userId, String shopId, String orderId, OrderFulfillmentPatchRequest request) {
        Order order = orderRepository.findByIdAndDeletedFalse(orderId)
                .filter(o -> o.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        if (request.getNote() != null) {
            order.setNote(StringUtils.hasText(request.getNote()) ? request.getNote().trim() : null);
        }
        if (request.getShippingCarrier() != null) {
            order.setShippingCarrier(
                    StringUtils.hasText(request.getShippingCarrier()) ? request.getShippingCarrier().trim() : null);
        }
        if (request.getShippingMethod() != null) {
            order.setShippingMethod(
                    StringUtils.hasText(request.getShippingMethod()) ? request.getShippingMethod().trim() : null);
        }
        if (request.getTrackingNumber() != null) {
            order.setTrackingNumber(
                    StringUtils.hasText(request.getTrackingNumber()) ? request.getTrackingNumber().trim() : null);
        }
        if (request.getExternalOrderRef() != null) {
            order.setExternalOrderRef(
                    StringUtils.hasText(request.getExternalOrderRef()) ? request.getExternalOrderRef().trim() : null);
        }
        if (request.getCustomerId() != null) {
            String cid = StringUtils.hasText(request.getCustomerId()) ? request.getCustomerId().trim() : null;
            if (cid != null) {
                customerRepository.findByIdAndDeletedFalse(cid)
                        .filter(c -> shopId.equals(c.getShopId()))
                        .orElseThrow(() -> new ResourceNotFoundException(ApiCode.CUSTOMER_NOT_FOUND));
            }
            order.setCustomerId(cid);
        }

        Order updated = orderRepository.save(order);
        orderCache.evict(orderId, shopId);
        auditLogService.log(userId, shopId, orderId, "ORDER", "FULFILLMENT_PATCHED", "Cập nhật giao hàng / tham chiếu đơn hàng");
        return toResponse(updated);
    }

    private OrderResponse toResponse(Order order) {
        Map<String, Product> productById = loadProductsForOrderLineEnrichment(order);
        Map<String, Promotion> promotionById = loadPromotionsForOrderLineEnrichment(order.getItems());
        String customerName = null;
        String customerPhone = null;
        if (StringUtils.hasText(order.getCustomerId()) && StringUtils.hasText(order.getShopId())) {
            var cOpt = customerRepository.findByIdAndDeletedFalse(order.getCustomerId());
            if (cOpt.isPresent()) {
                Customer c = cOpt.get();
                if (order.getShopId().equals(c.getShopId())) {
                    customerName = StringUtils.hasText(c.getName()) ? c.getName() : null;
                    customerPhone = StringUtils.hasText(c.getPhone()) ? c.getPhone() : null;
                }
            }
        }
        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(OrderDisplayUtils.displayOrderCode(order))
                .tableId(order.getTableId())
                .branchId(order.getBranchId()) // Thêm branchId vào response
                .note(order.getNote())
                .status(order.getStatus())
                .paid(order.isPaid())
                .paymentStatus(resolvePaymentStatus(order))
                .paymentMethod(order.getPaymentMethod())
                .paymentId(order.getPaymentId())
                .paymentTime(order.getPaymentTime())
                .totalAmount(order.getTotalAmount())
                .totalPrice(order.getTotalPrice())
                .items(order.getItems().stream()
                        .map(i -> toItemResponse(i, productById, promotionById))
                        .toList())
                .taxSnapshot(order.getTaxSnapshot())
                .customerId(order.getCustomerId())
                .customerName(customerName)
                .customerPhone(customerPhone)
                .pointsEarned(order.getPointsEarned())
                .pointsRedeemed(order.getPointsRedeemed())
                .pointsDiscount(order.getPointsDiscount())
                .shippingCarrier(order.getShippingCarrier())
                .shippingMethod(order.getShippingMethod())
                .trackingNumber(order.getTrackingNumber())
                .externalOrderRef(order.getExternalOrderRef())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private PaymentStatus resolvePaymentStatus(Order order) {
        if (order.getPaymentStatus() != null) {
            return order.getPaymentStatus();
        }
        return order.isPaid() ? PaymentStatus.PAID : PaymentStatus.UNPAID;
    }

    private Map<String, Product> loadProductsForOrderLineEnrichment(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return Map.of();
        }
        Set<String> productIds = order.getItems().stream()
                .map(OrderItem::getProductId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Product> map = new HashMap<>();
        for (String id : productIds) {
            productRepository.findByIdAndDeletedFalse(id).ifPresent(p -> map.put(id, p));
        }
        return map;
    }

    private Map<String, Promotion> loadPromotionsForOrderLineEnrichment(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return Map.of();
        }
        Set<String> promoIds = items.stream()
                .map(OrderItem::getAppliedPromotionId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (promoIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Promotion> map = new HashMap<>();
        for (String id : promoIds) {
            promotionRepository.findByIdAndDeletedFalse(id).ifPresent(p -> map.put(id, p));
        }
        return map;
    }

    private OrderItemResponse toItemResponse(
            OrderItem item,
            Map<String, Product> productById,
            Map<String, Promotion> promotionById) {
        String variantName = item.getVariantName();
        String sku = item.getSku();
        if (StringUtils.hasText(item.getVariantId())) {
            Product p = productById != null ? productById.get(item.getProductId()) : null;
            if (p != null) {
                String resolved = resolveVariantDisplayNameRich(p, item.getVariantId());
                if (!StringUtils.hasText(variantName) || isVariantCodeOnlyLabel(variantName)) {
                    variantName = resolved;
                }
                if (!StringUtils.hasText(sku)) {
                    sku = resolveLineSku(p, item.getVariantId());
                }
            } else if (!StringUtils.hasText(variantName)) {
                variantName = "Biến thể (mã): " + shortenVariantIdForDisplay(item.getVariantId());
            }
        }

        String promoName = item.getPromotionName();
        String promoLabel = item.getPromotionDiscountLabel();
        if (StringUtils.hasText(item.getAppliedPromotionId()) && promotionById != null) {
            Promotion pr = promotionById.get(item.getAppliedPromotionId());
            if (pr != null) {
                if (!StringUtils.hasText(promoName)) {
                    promoName = pr.getName();
                }
                if (!StringUtils.hasText(promoLabel)) {
                    promoLabel = formatPromotionDiscountLabel(pr);
                }
            }
        }

        return OrderItemResponse.builder()
                .productId(item.getProductId())
                .branchProductId(item.getBranchProductId()) // Thêm branchProductId vào response
                .variantId(item.getVariantId())
                .productName(item.getProductName())
                .variantName(variantName)
                .sku(sku)
                .promotionName(promoName)
                .promotionDiscountLabel(promoLabel)
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .priceAfterDiscount(item.getPriceAfterDiscount())
                .appliedPromotionId(item.getAppliedPromotionId())
                .build();
    }

    private static boolean isVariantCodeOnlyLabel(String variantName) {
        return variantName != null && variantName.startsWith("Biến thể (mã):");
    }

    private OrderItem buildOrderItemLine(
            Product masterProduct,
            BranchProduct branchProduct,
            String requestVariantId,
            int quantity,
            String shopId,
            String branchId) {
        validateOrderLineVariant(branchProduct, requestVariantId);
        String effectiveVariantId = hasTrackedVariants(branchProduct) ? requestVariantId : null;

        double basePrice = resolveLineBasePrice(branchProduct, requestVariantId);
        double finalPrice = basePrice;
        String promoId = null;
        String promoName = null;
        String promoLabel = null;

        Promotion promo = findApplicablePromotion(shopId, branchId, masterProduct.getId());
        if (promo != null) {
            promoId = promo.getId();
            promoName = promo.getName();
            promoLabel = formatPromotionDiscountLabel(promo);
            if (promo.getDiscountType() == DiscountType.PERCENT) {
                finalPrice = basePrice * (1 - promo.getDiscountValue() / 100.0);
            } else if (promo.getDiscountType() == DiscountType.AMOUNT) {
                finalPrice = Math.max(0, basePrice - promo.getDiscountValue());
            }
        }

        String variantName = resolveVariantDisplayNameRich(masterProduct, effectiveVariantId);
        String sku = resolveLineSku(masterProduct, effectiveVariantId);

        return OrderItem.builder()
                .productId(masterProduct.getId())
                .branchProductId(branchProduct.getId())
                .variantId(effectiveVariantId)
                .productName(masterProduct.getName())
                .variantName(variantName)
                .sku(sku)
                .quantity(quantity)
                .price(basePrice)
                .priceAfterDiscount(finalPrice)
                .appliedPromotionId(promoId)
                .promotionName(promoName)
                .promotionDiscountLabel(promoLabel)
                .trackInventory(masterProduct.isTrackInventory())
                .build();
    }

    /**
     * Tên hiển thị biến thể: ưu tiên tên master → thuộc tính → mã rút gọn.
     * Khớp linh hoạt (không phân biệt hoa thường, bỏ dấu -, hậu tố 8 ký tự) vì POS/DB có thể lưu khác nhau.
     */
    private String resolveVariantDisplayNameRich(Product master, String variantId) {
        if (!StringUtils.hasText(variantId)) {
            return null;
        }
        Optional<ProductVariant> ov = findProductVariant(master, variantId);
        if (ov.isEmpty()) {
            return "Biến thể (mã): " + shortenVariantIdForDisplay(variantId);
        }
        ProductVariant v = ov.get();
        if (StringUtils.hasText(v.getName())) {
            return v.getName();
        }
        String fromAttrs = formatVariantAttributes(v.getAttributes());
        if (StringUtils.hasText(fromAttrs)) {
            return fromAttrs;
        }
        return "Biến thể (mã): " + shortenVariantIdForDisplay(variantId);
    }

    private Optional<ProductVariant> findProductVariant(Product master, String variantId) {
        if (!StringUtils.hasText(variantId) || master.getVariants() == null || master.getVariants().isEmpty()) {
            return Optional.empty();
        }
        String raw = variantId.trim();
        List<ProductVariant> list = master.getVariants();
        Optional<ProductVariant> exact = list.stream()
                .filter(v -> v.getVariantId() != null && raw.equals(v.getVariantId().trim()))
                .findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        Optional<ProductVariant> ignoreCase = list.stream()
                .filter(v -> v.getVariantId() != null && raw.equalsIgnoreCase(v.getVariantId().trim()))
                .findFirst();
        if (ignoreCase.isPresent()) {
            return ignoreCase;
        }
        String normOrder = normalizeVariantId(raw);
        Optional<ProductVariant> normMatch = list.stream()
                .filter(v -> v.getVariantId() != null)
                .filter(v -> normalizeVariantId(v.getVariantId()).equals(normOrder))
                .findFirst();
        if (normMatch.isPresent()) {
            return normMatch;
        }
        if (normOrder.length() <= 12) {
            return list.stream()
                    .filter(v -> v.getVariantId() != null)
                    .filter(v -> {
                        String nv = normalizeVariantId(v.getVariantId());
                        return nv.endsWith(normOrder) || normOrder.endsWith(nv);
                    })
                    .findFirst();
        }
        return Optional.empty();
    }

    private static String normalizeVariantId(String id) {
        return id.trim().replace("-", "").toLowerCase(Locale.ROOT);
    }

    private static String shortenVariantIdForDisplay(String variantId) {
        String t = variantId.trim();
        if (t.length() <= 10) {
            return t.toUpperCase();
        }
        return t.substring(t.length() - 8).toUpperCase();
    }

    private String formatVariantAttributes(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return null;
        }
        return attributes.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
    }

    private String resolveLineSku(Product master, String variantId) {
        if (!StringUtils.hasText(variantId)) {
            return master.getSku();
        }
        return findProductVariant(master, variantId)
                .map(v -> StringUtils.hasText(v.getSku()) ? v.getSku() : master.getSku())
                .orElse(master.getSku());
    }

    private String formatPromotionDiscountLabel(Promotion promo) {
        if (promo == null) {
            return null;
        }
        if (promo.getDiscountType() == DiscountType.PERCENT) {
            double v = promo.getDiscountValue();
            String s = (v == Math.floor(v)) ? String.valueOf((long) v) : String.valueOf(v);
            return s + "%";
        }
        return (long) promo.getDiscountValue() + " ₫";
    }

    private String generateOrderCode(String shopId) {
        String raw = sequenceService.getNextCode(shopId, "DH", AppConstants.SequenceTypes.SEQUENCE_TYPE_ORDER);
        sequenceService.updateNextSequence(shopId, "DH", AppConstants.SequenceTypes.SEQUENCE_TYPE_ORDER);
        String tail = raw.contains("_") ? raw.substring(raw.lastIndexOf('_') + 1) : raw;
        return "DH-" + LocalDate.now().format(ORDER_CODE_DATE) + "-" + tail;
    }

}