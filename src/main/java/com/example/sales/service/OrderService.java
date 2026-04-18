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
import com.example.sales.dto.order.OrderLineToppingResponse;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
    private final TableGroupRepository tableGroupRepository;
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
        order.setGuestName(StringUtils.hasText(request.getGuestName()) ? request.getGuestName().trim() : null);
        order.setGuestPhone(StringUtils.hasText(request.getGuestPhone()) ? request.getGuestPhone().trim() : null);
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

        Shop orderShop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.SHOP_NOT_FOUND));

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
                    orderShop,
                    masterProduct,
                    branchProduct,
                    reqItem.getVariantId(),
                    reqItem.getToppingIds(),
                    reqItem.getQuantity(),
                    shopId,
                    branchId);

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
        Order saved = orderRepository.save(order);
        // Giải phóng bàn khi huỷ đơn (đơn chưa thanh toán)
        releaseTable(saved);
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
        releasePaidTableFromTableGroups(userId, shopId, updated);

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
        if (newStatus == OrderStatus.COMPLETED && updated.isPaid()) {
            releasePaidTableFromTableGroups(userId, shopId, updated);
        }
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
                // Bàn “luôn trống”: không OCCUPIED, không gắn currentOrderId (nhiều đơn song song).
                if (Boolean.TRUE.equals(table.getAlwaysAvailable())) {
                    return;
                }
                table.setStatus(TableStatus.OCCUPIED);
                table.setCurrentOrderId(order.getId());
                tableRepository.save(table);
            });
        }
    }

    private void releaseTable(Order order) {
        if (order.getTableId() != null && !order.getTableId().isBlank()) {
            tableRepository.findByIdAndDeletedFalse(order.getTableId()).ifPresent(table -> {
                boolean always = Boolean.TRUE.equals(table.getAlwaysAvailable());
                String cur = StringUtils.hasText(table.getCurrentOrderId())
                        ? table.getCurrentOrderId().trim()
                        : null;
                // Bàn luôn trống: chỉ xóa pointer nếu đúng đơn này (tránh gỡ nhầm khi DB còn dữ liệu cũ).
                if (always && cur != null && !order.getId().equals(cur)) {
                    return;
                }
                table.setCurrentOrderId(null);
                if (!always) {
                    table.setStatus(TableStatus.AVAILABLE);
                } else if (table.getStatus() == null) {
                    table.setStatus(TableStatus.AVAILABLE);
                }
                tableRepository.save(table);
            });
        }
    }

    /**
     * Sau thanh toán: gỡ bàn của đơn khỏi mọi {@link TableGroup} (cùng shop/branch).
     * Nếu nhóm còn dưới 2 bàn thì xóa (soft-delete) cả nhóm.
     */
    private void releasePaidTableFromTableGroups(String userId, String shopId, Order order) {
        if (order == null || !StringUtils.hasText(order.getTableId()) || !StringUtils.hasText(order.getBranchId())) {
            return;
        }
        String tableId = order.getTableId().trim();
        String branchId = order.getBranchId().trim();
        List<TableGroup> groups = tableGroupRepository
                .findByShopIdAndBranchIdAndDeletedFalseAndTableIdsContains(shopId, branchId, tableId);
        if (groups == null || groups.isEmpty()) {
            return;
        }
        for (TableGroup g : groups) {
            if (g.getTableIds() == null || g.getTableIds().isEmpty()) {
                continue;
            }
            List<String> next = g.getTableIds().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .filter(id -> !id.equals(tableId))
                    .distinct()
                    .toList();
            if (next.size() < 2) {
                g.setDeleted(true);
                tableGroupRepository.save(g);
                auditLogService.log(userId, shopId, g.getId(), "TABLE_GROUP", "DELETED",
                        "Giải nhóm tự động sau thanh toán (còn %d bàn)".formatted(next.size()));
            } else {
                g.setTableIds(new ArrayList<>(next));
                tableGroupRepository.save(g);
                auditLogService.log(userId, shopId, g.getId(), "TABLE_GROUP", "UPDATED",
                        "Gỡ bàn khỏi nhóm sau thanh toán: %s".formatted(tableId));
            }
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

    public OrderResponse getOrderById(String shopId, String orderId) {
        Order order = orderCache.getOrderByShop(orderId, shopId);
        return toResponse(order);
    }

    /**
     * Mở đơn POS theo orderCode hoặc id đơn: chỉ đơn chưa thanh toán; nếu branchId có thì phải khớp chi nhánh đơn.
     */
    public OrderResponse lookupOrderForPosEdit(String shopId, String branchId, String orderCode, String orderId) {
        Order order;
        if (StringUtils.hasText(orderCode)) {
            String trimmed = orderCode.trim();
            order = orderRepository.findByShopIdAndOrderCodeIgnoreCaseAndDeletedFalse(shopId, trimmed)
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.ORDER_NOT_FOUND));
        } else if (StringUtils.hasText(orderId)) {
            order = orderCache.getOrderByShop(orderId.trim(), shopId);
        } else {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        if (order.isPaid()) {
            throw new BusinessException(ApiCode.ORDER_ALREADY_PAID);
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        if (StringUtils.hasText(branchId) && StringUtils.hasText(order.getBranchId())
                && !branchId.equals(order.getBranchId())) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        return toResponse(order);
    }

    public Page<OrderResponse> getOpenOrders(String shopId, String branchId, Pageable pageable) {
        if (!StringUtils.hasText(branchId)) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        List<OrderStatus> excluded = List.of(OrderStatus.CANCELLED, OrderStatus.COMPLETED);
        return orderRepository
                .findOpenOrdersByShopIdAndBranchId(shopId, branchId, excluded, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public OrderResponse moveTable(String userId, String shopId, String orderId, String toTableId) {
        Order order = orderCache.getOrderByShop(orderId, shopId);
        if (order.isPaid()) {
            throw new BusinessException(ApiCode.ORDER_ALREADY_PAID);
        }
        if (!StringUtils.hasText(toTableId)) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        Table toTable = tableRepository.findByIdAndDeletedFalse(toTableId.trim())
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND));
        if (!shopId.equals(toTable.getShopId())) {
            throw new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND);
        }
        // Khóa theo branch: chỉ cho đổi trong cùng chi nhánh của order
        if (StringUtils.hasText(order.getBranchId())
                && StringUtils.hasText(toTable.getBranchId())
                && !order.getBranchId().equals(toTable.getBranchId())) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        String current = toTable.getCurrentOrderId();
        boolean alwaysAvail = Boolean.TRUE.equals(toTable.getAlwaysAvailable());
        if (!alwaysAvail
                && StringUtils.hasText(current)
                && !orderId.equals(current.trim())) {
            throw new BusinessException(ApiCode.TABLE_OCCUPIED);
        }

        String fromTableId = order.getTableId();
        if (StringUtils.hasText(fromTableId) && fromTableId.trim().equals(toTable.getId())) {
            return toResponse(order);
        }

        // Release old table if any
        if (StringUtils.hasText(fromTableId)) {
            tableRepository.findByIdAndDeletedFalse(fromTableId.trim()).ifPresent(t -> {
                if (shopId.equals(t.getShopId()) && orderId.equals(t.getCurrentOrderId())) {
                    t.setCurrentOrderId(null);
                    if (!Boolean.TRUE.equals(t.getAlwaysAvailable())) {
                        t.setStatus(TableStatus.AVAILABLE);
                    } else if (t.getStatus() == null) {
                        t.setStatus(TableStatus.AVAILABLE);
                    }
                    tableRepository.save(t);
                }
            });
        }

        order.setTableId(toTable.getId());
        Order saved = orderRepository.save(order);
        // Occupy new table (respect "always available" tables)
        occupyTable(saved);
        orderCache.evict(orderId, shopId);
        auditLogService.log(userId, shopId, orderId, "ORDER", "TABLE_MOVED",
                "Đổi bàn cho đơn hàng từ %s sang %s".formatted(fromTableId, toTable.getId()));
        return toResponse(saved);
    }

    @Transactional
    public Map<String, OrderResponse> splitOrder(String userId, String shopId, String orderId,
                                                 com.example.sales.dto.order.OrderSplitRequest request) {
        Order src = orderCache.getOrderByShop(orderId, shopId);
        if (src.isPaid()) {
            throw new BusinessException(ApiCode.ORDER_ALREADY_PAID);
        }
        if (src.getStatus() == OrderStatus.CANCELLED || src.getStatus() == OrderStatus.COMPLETED) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        if (src.getPointsRedeemed() > 0 || src.getPointsDiscount() > 0) {
            // Split with loyalty discount is ambiguous; keep it simple for now.
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        if (request == null || request.getItemsToMove() == null || request.getItemsToMove().isEmpty()) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        // Validate destination table if provided
        String toTableId = StringUtils.hasText(request.getToTableId()) ? request.getToTableId().trim() : null;
        if (toTableId != null) {
            Table toTable = tableRepository.findByIdAndDeletedFalse(toTableId)
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND));
            if (!shopId.equals(toTable.getShopId()) || !src.getBranchId().equals(toTable.getBranchId())) {
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }
            if (!Boolean.TRUE.equals(toTable.getAlwaysAvailable())
                    && StringUtils.hasText(toTable.getCurrentOrderId())) {
                throw new BusinessException(ApiCode.TABLE_OCCUPIED);
            }
        }

        // Validate move list and compute remaining/moved items
        Map<String, Integer> moveQtyByKey = new HashMap<>();
        for (var mv : request.getItemsToMove()) {
            String k = splitLineKey(mv.getProductId(), mv.getVariantId(), mv.getToppingIds());
            moveQtyByKey.merge(k, mv.getQuantity(), Integer::sum);
        }

        List<OrderItem> moved = new java.util.ArrayList<>();
        List<OrderItem> remaining = new java.util.ArrayList<>();

        for (OrderItem it : (src.getItems() == null ? List.<OrderItem>of() : src.getItems())) {
            String k = splitLineKey(it.getProductId(), it.getVariantId(), toppingIdsFromSnapshots(it.getToppings()));
            int moveQty = moveQtyByKey.getOrDefault(k, 0);
            if (moveQty <= 0) {
                remaining.add(it);
                continue;
            }
            if (moveQty > it.getQuantity()) {
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }
            if (moveQty < it.getQuantity()) {
                OrderItem kept = OrderItem.builder()
                        .productId(it.getProductId())
                        .branchProductId(it.getBranchProductId())
                        .variantId(it.getVariantId())
                        .productName(it.getProductName())
                        .variantName(it.getVariantName())
                        .sku(it.getSku())
                        .promotionName(it.getPromotionName())
                        .promotionDiscountLabel(it.getPromotionDiscountLabel())
                        .quantity(it.getQuantity() - moveQty)
                        .price(it.getPrice())
                        .priceAfterDiscount(it.getPriceAfterDiscount())
                        .appliedPromotionId(it.getAppliedPromotionId())
                        .trackInventory(it.isTrackInventory())
                        .toppings(it.getToppings())
                        .build();
                remaining.add(kept);
            }
            OrderItem movedLine = OrderItem.builder()
                    .productId(it.getProductId())
                    .branchProductId(it.getBranchProductId())
                    .variantId(it.getVariantId())
                    .productName(it.getProductName())
                    .variantName(it.getVariantName())
                    .sku(it.getSku())
                    .promotionName(it.getPromotionName())
                    .promotionDiscountLabel(it.getPromotionDiscountLabel())
                    .quantity(moveQty)
                    .price(it.getPrice())
                    .priceAfterDiscount(it.getPriceAfterDiscount())
                    .appliedPromotionId(it.getAppliedPromotionId())
                    .trackInventory(it.isTrackInventory())
                    .toppings(it.getToppings())
                    .build();
            moved.add(movedLine);
        }

        if (moved.isEmpty() || remaining.isEmpty()) {
            // Disallow moving all items for now
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        // Create new order for moved items (re-export inventory for moved items)
        Order newOrder = new Order();
        newOrder.setShopId(shopId);
        newOrder.setBranchId(src.getBranchId());
        newOrder.setUserId(userId);
        newOrder.setTableId(toTableId);
        newOrder.setNote(src.getNote());
        newOrder.setStatus(src.getStatus());
        newOrder.setPaid(false);
        newOrder.setPaymentStatus(PaymentStatus.UNPAID);
        newOrder.setOrderCode(generateOrderCode(shopId));
        newOrder.setItems(moved);

        double[] totalsNew = {0, 0};
        for (OrderItem it : moved) {
            totalsNew[0] += it.getQuantity();
            totalsNew[1] += it.getQuantity() * it.getPriceAfterDiscount();
        }
        newOrder.setTotalPrice(totalsNew[1]);
        orderTaxApplier.applyTax(newOrder);
        Order created = orderRepository.save(newOrder);

        for (OrderItem it : created.getItems()) {
            if (it.isTrackInventory()) {
                inventoryService.exportProductQuantity(
                        userId, shopId, created.getBranchId(), it.getBranchProductId(),
                        it.getVariantId(),
                        it.getQuantity(),
                        "Xuất kho theo tách đơn " + OrderDisplayUtils.displayOrderCode(created),
                        created.getId());
            }
        }
        occupyTable(created);

        // Update original order using existing logic (imports old, exports remaining)
        com.example.sales.dto.order.OrderUpdateRequest upd = com.example.sales.dto.order.OrderUpdateRequest.builder()
                .tableId(src.getTableId())
                .note(src.getNote())
                .items(remaining.stream().map(it -> com.example.sales.dto.order.OrderUpdateRequest.OrderItemUpdateRequest.builder()
                        .productId(it.getProductId())
                        .variantId(it.getVariantId())
                        .quantity(it.getQuantity())
                        .price(it.getPrice())
                        .toppingIds(toppingIdsFromSnapshots(it.getToppings()))
                        .build()).toList())
                .build();

        OrderResponse srcUpdated = updateOrder(userId, shopId, orderId, upd);
        OrderResponse newResp = toResponse(created);

        auditLogService.log(userId, shopId, orderId, "ORDER", "SPLIT",
                "Tách %d dòng hàng sang đơn %s".formatted(moved.size(), created.getId()));
        orderCache.evict(orderId, shopId);
        return Map.of("source", srcUpdated, "newOrder", newResp);
    }

    /**
     * Gộp nhiều đơn đang mở của các bàn trong cùng một {@link TableGroup} về một đơn đích.
     * Các đơn nguồn sẽ bị huỷ (hoàn kho theo luồng huỷ đơn), sau đó đơn đích được cập nhật lại dòng hàng.
     */
    @Transactional
    public OrderResponse mergeTableGroupOrders(
            String userId, String shopId, com.example.sales.dto.order.OrderGroupMergeRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getTargetOrderId())
                || request.getSourceOrderIds() == null
                || request.getSourceOrderIds().isEmpty()) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        String targetId = request.getTargetOrderId().trim();
        List<String> distinctIds = request.getSourceOrderIds().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (distinctIds.size() < 2) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        if (!distinctIds.contains(targetId)) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        List<OrderStatus> excluded = List.of(OrderStatus.CANCELLED, OrderStatus.COMPLETED);
        List<Order> loaded = orderRepository.findOpenOrdersByShopIdAndIdIn(shopId, excluded, distinctIds);
        Map<String, Order> byId = loaded.stream().collect(Collectors.toMap(Order::getId, o -> o, (a, b) -> a));
        if (byId.size() != distinctIds.size()) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        Order target = byId.get(targetId);
        if (target == null || !shopId.equals(target.getShopId())) {
            throw new ResourceNotFoundException(ApiCode.ORDER_NOT_FOUND);
        }
        if (!StringUtils.hasText(target.getBranchId())) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        String branchId = target.getBranchId();

        for (String id : distinctIds) {
            Order o = byId.get(id);
            if (o == null || !shopId.equals(o.getShopId()) || !branchId.equals(o.getBranchId())) {
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }
            if (o.isPaid()) {
                throw new BusinessException(ApiCode.ORDER_ALREADY_PAID);
            }
            if (o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.COMPLETED) {
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }
            if (o.getPointsRedeemed() > 0 || o.getPointsDiscount() > 0) {
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }
        }

        // Customer consistency (OrderUpdateRequest cannot merge customer fields safely)
        String customerId = StringUtils.hasText(target.getCustomerId()) ? target.getCustomerId().trim() : null;
        for (String id : distinctIds) {
            Order o = byId.get(id);
            String cid = StringUtils.hasText(o.getCustomerId()) ? o.getCustomerId().trim() : null;
            if (cid == null) continue;
            if (customerId == null) {
                customerId = cid;
                continue;
            }
            if (!customerId.equals(cid)) {
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }
        }

        java.util.LinkedHashSet<String> tableIds = new java.util.LinkedHashSet<>();
        for (String id : distinctIds) {
            Order o = byId.get(id);
            if (!StringUtils.hasText(o.getTableId())) {
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }
            tableIds.add(o.getTableId().trim());
        }

        TableGroup group = null;
        for (TableGroup g : tableGroupRepository.findByShopIdAndBranchIdAndDeletedFalse(shopId, branchId)) {
            if (g.getTableIds() == null || g.getTableIds().size() < 2) continue;
            java.util.HashSet<String> gid = g.getTableIds().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .collect(Collectors.toCollection(java.util.HashSet::new));
            boolean allInGroup = tableIds.stream().allMatch(gid::contains);
            if (allInGroup) {
                group = g;
                break;
            }
        }
        if (group == null) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        // Snapshot lines BEFORE cancelling sources (cancel mutates persisted orders)
        java.util.Map<String, Integer> qtyByKey = new java.util.HashMap<>();
        java.util.Map<String, String> productIdByKey = new java.util.HashMap<>();
        java.util.Map<String, String> variantIdByKey = new java.util.HashMap<>();
        java.util.Map<String, List<String>> toppingIdsByKey = new java.util.HashMap<>();
        for (String id : distinctIds) {
            Order o = byId.get(id);
            for (OrderItem it : (o.getItems() == null ? List.<OrderItem>of() : o.getItems())) {
                if (!StringUtils.hasText(it.getProductId())) {
                    throw new BusinessException(ApiCode.VALIDATION_ERROR);
                }
                String key = mergeLineKey(it);
                qtyByKey.merge(key, it.getQuantity(), Integer::sum);
                productIdByKey.putIfAbsent(key, it.getProductId().trim());
                String vid = it.getVariantId();
                variantIdByKey.putIfAbsent(key, StringUtils.hasText(vid) ? vid.trim() : "");
                toppingIdsByKey.putIfAbsent(key, toppingIdsFromSnapshots(it.getToppings()));
            }
        }

        // Cancel non-target orders first (refund inventory for those lines)
        for (String id : distinctIds) {
            if (id.equals(targetId)) continue;
            cancelOrder(userId, shopId, id);
        }

        java.util.List<OrderUpdateRequest.OrderItemUpdateRequest> updItems = new java.util.ArrayList<>();
        for (var e : qtyByKey.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            String key = e.getKey();
            String productId = productIdByKey.get(key);
            String variantStored = variantIdByKey.getOrDefault(key, "");
            String variantId = StringUtils.hasText(variantStored) ? variantStored : null;
            if (!StringUtils.hasText(productId)) {
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }
            updItems.add(OrderUpdateRequest.OrderItemUpdateRequest.builder()
                    .productId(productId)
                    .variantId(variantId)
                    .quantity(e.getValue())
                    .price(0)
                    .toppingIds(toppingIdsByKey.get(key))
                    .build());
        }
        if (updItems.isEmpty()) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        String mergedNote = target.getNote();
        OrderUpdateRequest upd = OrderUpdateRequest.builder()
                .tableId(target.getTableId())
                .note(mergedNote)
                .items(updItems)
                .build();

        OrderResponse updated = updateOrder(userId, shopId, targetId, upd);
        auditLogService.log(userId, shopId, targetId, "ORDER", "MERGED_GROUP",
                "Gộp bill nhóm (%s): %s".formatted(group.getId(), String.join(",", distinctIds)));
        return updated;
    }

    private static String mergeLineKey(OrderItem it) {
        String pid = it.getProductId() == null ? "" : it.getProductId().trim();
        String vid = it.getVariantId() == null ? "" : it.getVariantId().trim();
        String bp = it.getBranchProductId() == null ? "" : it.getBranchProductId().trim();
        String prom = it.getAppliedPromotionId() == null ? "" : it.getAppliedPromotionId().trim();
        String tp = toppingKeyFromSnapshots(it.getToppings());
        // Keep separate buckets if pricing/promo differs (even for same product/variant)
        return pid
                + "###v=" + vid
                + "###bp=" + bp
                + "###p=" + it.getPrice()
                + "###pad=" + it.getPriceAfterDiscount()
                + "###prom=" + prom
                + "###tp=" + tp;
    }

    private static String splitLineKey(String productId, String variantId, List<String> toppingIds) {
        String pid = productId == null ? "" : productId.trim();
        String vid = variantId == null ? "" : variantId.trim();
        String tk = toppingKeyFromIdList(toppingIds);
        return pid + "||" + vid + "||" + tk;
    }

    private static String toppingKeyFromIdList(List<String> toppingIds) {
        if (toppingIds == null || toppingIds.isEmpty()) {
            return "";
        }
        return toppingIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .sorted()
                .collect(Collectors.joining(","));
    }

    private static String toppingKeyFromSnapshots(List<OrderLineTopping> toppings) {
        if (toppings == null || toppings.isEmpty()) {
            return "";
        }
        return toppings.stream()
                .map(OrderLineTopping::getToppingId)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .sorted()
                .collect(Collectors.joining(","));
    }

    private static List<String> toppingIdsFromSnapshots(List<OrderLineTopping> toppings) {
        if (toppings == null || toppings.isEmpty()) {
            return List.of();
        }
        return toppings.stream()
                .map(OrderLineTopping::getToppingId)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .sorted()
                .toList();
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

        if (request.getGuestName() != null) {
            order.setGuestName(StringUtils.hasText(request.getGuestName()) ? request.getGuestName().trim() : null);
        }
        if (request.getGuestPhone() != null) {
            order.setGuestPhone(StringUtils.hasText(request.getGuestPhone()) ? request.getGuestPhone().trim() : null);
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

            Shop orderShopForItems = shopRepository.findByIdAndDeletedFalse(shopId)
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.SHOP_NOT_FOUND));

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
                        orderShopForItems,
                        masterProduct,
                        branchProduct,
                        reqItem.getVariantId(),
                        reqItem.getToppingIds(),
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
        if (request.getGuestName() != null) {
            order.setGuestName(
                    StringUtils.hasText(request.getGuestName()) ? request.getGuestName().trim() : null);
        }
        if (request.getGuestPhone() != null) {
            order.setGuestPhone(
                    StringUtils.hasText(request.getGuestPhone()) ? request.getGuestPhone().trim() : null);
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
                .guestName(order.getGuestName())
                .guestPhone(order.getGuestPhone())
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
        String variantAttributesText = null;
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
                variantAttributesText = findProductVariant(p, item.getVariantId())
                        .map(v -> formatVariantAttributes(v.getAttributes()))
                        .filter(StringUtils::hasText)
                        .orElse(null);
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

        List<OrderLineToppingResponse> toppingResp = null;
        if (item.getToppings() != null && !item.getToppings().isEmpty()) {
            toppingResp = item.getToppings().stream()
                    .map(t -> OrderLineToppingResponse.builder()
                            .toppingId(t.getToppingId())
                            .name(t.getName())
                            .extraPrice(t.getExtraPrice())
                            .build())
                    .toList();
        }

        return OrderItemResponse.builder()
                .productId(item.getProductId())
                .branchProductId(item.getBranchProductId()) // Thêm branchProductId vào response
                .variantId(item.getVariantId())
                .productName(item.getProductName())
                .variantName(variantName)
                .variantAttributesText(variantAttributesText)
                .sku(sku)
                .promotionName(promoName)
                .promotionDiscountLabel(promoLabel)
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .priceAfterDiscount(item.getPriceAfterDiscount())
                .appliedPromotionId(item.getAppliedPromotionId())
                .toppings(toppingResp)
                .build();
    }

    private static boolean isVariantCodeOnlyLabel(String variantName) {
        return variantName != null && variantName.startsWith("Biến thể (mã):");
    }

    private OrderItem buildOrderItemLine(
            Shop shop,
            Product masterProduct,
            BranchProduct branchProduct,
            String requestVariantId,
            List<String> requestToppingIds,
            int quantity,
            String shopId,
            String branchId) {
        validateOrderLineVariant(branchProduct, requestVariantId);
        String effectiveVariantId = hasTrackedVariants(branchProduct) ? requestVariantId : null;

        List<OrderLineTopping> toppingSnapshots = resolveToppingSnapshots(shop, masterProduct, requestToppingIds);
        double toppingUnitSum = toppingSnapshots.stream().mapToDouble(OrderLineTopping::getExtraPrice).sum();

        double baseVariantUnit = resolveLineBasePrice(branchProduct, requestVariantId);
        double unitBeforePromo = baseVariantUnit + toppingUnitSum;
        double finalPrice = unitBeforePromo;
        String promoId = null;
        String promoName = null;
        String promoLabel = null;

        Promotion promo = findApplicablePromotion(shopId, branchId, masterProduct.getId());
        if (promo != null) {
            promoId = promo.getId();
            promoName = promo.getName();
            promoLabel = formatPromotionDiscountLabel(promo);
            if (promo.getDiscountType() == DiscountType.PERCENT) {
                finalPrice = unitBeforePromo * (1 - promo.getDiscountValue() / 100.0);
            } else if (promo.getDiscountType() == DiscountType.AMOUNT) {
                finalPrice = Math.max(0, unitBeforePromo - promo.getDiscountValue());
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
                .price(unitBeforePromo)
                .priceAfterDiscount(finalPrice)
                .appliedPromotionId(promoId)
                .promotionName(promoName)
                .promotionDiscountLabel(promoLabel)
                .trackInventory(masterProduct.isTrackInventory())
                .toppings(toppingSnapshots.isEmpty() ? null : toppingSnapshots)
                .build();
    }

    private List<OrderLineTopping> resolveToppingSnapshots(Shop shop, Product product, List<String> requestedRaw) {
        if (requestedRaw == null || requestedRaw.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String raw : requestedRaw) {
            if (StringUtils.hasText(raw)) {
                unique.add(raw.trim());
            }
        }
        if (unique.isEmpty()) {
            return List.of();
        }
        if (shop == null || !shop.isToppingsEnabled()) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        List<ShopTopping> defs = shop.getShopToppings();
        if (defs == null || defs.isEmpty()) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        List<String> assigned = product.getAssignedToppingIds();
        if (assigned == null || assigned.isEmpty()) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        Set<String> allowedLower = assigned.stream()
                .filter(StringUtils::hasText)
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<OrderLineTopping> out = new ArrayList<>();
        for (String tid : unique) {
            ShopTopping def = findShopTopping(defs, tid)
                    .orElseThrow(() -> new BusinessException(ApiCode.VALIDATION_ERROR));
            if (!def.isActive()) {
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }
            String defKey = def.getToppingId() != null ? def.getToppingId().trim().toLowerCase(Locale.ROOT) : "";
            if (!allowedLower.contains(defKey)) {
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }
            out.add(OrderLineTopping.builder()
                    .toppingId(def.getToppingId())
                    .name(def.getName())
                    .extraPrice(def.getExtraPrice())
                    .build());
        }
        out.sort(Comparator.comparing(OrderLineTopping::getToppingId, Comparator.nullsLast(String::compareToIgnoreCase)));
        return out;
    }

    private Optional<ShopTopping> findShopTopping(List<ShopTopping> defs, String toppingId) {
        if (!StringUtils.hasText(toppingId) || defs == null || defs.isEmpty()) {
            return Optional.empty();
        }
        String raw = toppingId.trim();
        Optional<ShopTopping> exact = defs.stream()
                .filter(t -> t.getToppingId() != null && raw.equals(t.getToppingId().trim()))
                .findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        return defs.stream()
                .filter(t -> t.getToppingId() != null && raw.equalsIgnoreCase(t.getToppingId().trim()))
                .findFirst();
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