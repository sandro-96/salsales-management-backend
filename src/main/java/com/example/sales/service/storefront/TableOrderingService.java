// File: src/main/java/com/example/sales/service/storefront/TableOrderingService.java
package com.example.sales.service.storefront;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.OrderSource;
import com.example.sales.constant.OrderStatus;
import com.example.sales.constant.PaymentStatus;
import com.example.sales.constant.TableStatus;
import com.example.sales.constant.WebSocketMessageType;
import com.example.sales.dto.storefront.TableContextResponse;
import com.example.sales.dto.storefront.TableOrderRequest;
import com.example.sales.dto.storefront.TableOrderResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Branch;
import com.example.sales.model.Order;
import com.example.sales.model.OrderItem;
import com.example.sales.model.Shop;
import com.example.sales.model.Table;
import com.example.sales.repository.BranchRepository;
import com.example.sales.repository.OrderRepository;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.TableRepository;
import com.example.sales.service.notification.OnlineOrderNotifier;
import com.example.sales.service.realtime.RealtimeEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service cho QR self-ordering tại bàn (dine-in).
 *
 * <p>Khác với storefront giao hàng ({@link StorefrontService}):
 * <ul>
 *   <li>Mỗi bàn duy trì <b>1 đơn mở duy nhất</b> trỏ qua {@link Table#getCurrentOrderId()}.
 *       Quét lại QR + gửi thêm món → append vào đơn đó (gộp tab).</li>
 *   <li>Không yêu cầu địa chỉ / phone khách (dine-in).</li>
 *   <li>Đơn ở trạng thái PENDING + paid=false; staff thu tiền tại quầy qua POS như đơn nội bộ.</li>
 *   <li>Bàn {@code alwaysAvailable=true} (Mang đi): mỗi lần quét tạo đơn mới, không gắn pointer.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableOrderingService {

    /** Trạng thái coi đơn là đã đóng (không append được nữa). */
    private static final List<OrderStatus> CLOSED_STATUSES =
            List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED);

    private final TableRepository tableRepository;
    private final ShopRepository shopRepository;
    private final BranchRepository branchRepository;
    private final OrderRepository orderRepository;
    private final StorefrontService storefrontService;
    private final OnlineOrderNotifier onlineOrderNotifier;
    private final RealtimeEventPublisher realtimeEventPublisher;

    /** Resolve {@code (shop, table, branch)} từ qrToken; ném lỗi nếu shop/table tắt QR. */
    public ResolvedTable resolve(String qrToken) {
        if (!StringUtils.hasText(qrToken)) {
            throw new ResourceNotFoundException(ApiCode.TABLE_QR_NOT_FOUND);
        }
        Table table = tableRepository.findByQrTokenAndDeletedFalse(qrToken.trim())
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TABLE_QR_NOT_FOUND));

        if (table.getStatus() == TableStatus.CLOSED) {
            throw new BusinessException(ApiCode.TABLE_INACTIVE);
        }
        if (!table.isQrOrderingEnabled()) {
            throw new BusinessException(ApiCode.TABLE_ORDERING_DISABLED);
        }

        Shop shop = shopRepository.findByIdAndDeletedFalse(table.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.SHOP_NOT_FOUND));
        if (!shop.isActive() || !shop.isTableOrderingEnabled()) {
            throw new BusinessException(ApiCode.TABLE_ORDERING_DISABLED);
        }

        Branch branch = StringUtils.hasText(table.getBranchId())
                ? branchRepository.findByIdAndShopIdAndDeletedFalse(table.getBranchId(), shop.getId())
                        .orElse(null)
                : null;
        return new ResolvedTable(shop, table, branch);
    }

    /**
     * Trả về context để FE render trang menu khách: thông tin shop (header), bàn (tên,
     * sức chứa), branch (địa chỉ).
     */
    public TableContextResponse getContext(String qrToken) {
        ResolvedTable rt = resolve(qrToken);
        return TableContextResponse.builder()
                .shop(buildShopResponse(rt.shop()))
                .table(TableContextResponse.TableInfo.builder()
                        .id(rt.table().getId())
                        .name(rt.table().getName())
                        .branchId(rt.table().getBranchId())
                        .branchName(rt.branch() != null ? rt.branch().getName() : null)
                        .branchAddress(rt.branch() != null ? rt.branch().getAddress() : null)
                        .capacity(rt.table().getCapacity())
                        .build())
                .build();
    }

    /** Trả đơn đang mở của bàn (nếu có) — guest poll/refresh sau khi gửi món. */
    public TableOrderResponse getCurrentOrder(String qrToken) {
        ResolvedTable rt = resolve(qrToken);
        Order order = findCurrentOpenOrder(rt.table());
        return order == null ? null : toOrderResponse(order);
    }

    /**
     * Append items vào đơn đang mở của bàn; nếu chưa có thì tạo đơn mới.
     * Trả về cặp {@code (response, wasCreated)} để FE/Controller biết dùng ApiCode phù hợp.
     */
    @Transactional
    public CreatedOrAppended placeOrder(String qrToken, TableOrderRequest request) {
        ResolvedTable rt = resolve(qrToken);
        Shop shop = rt.shop();
        Table table = rt.table();

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(ApiCode.STOREFRONT_EMPTY_CART);
        }

        StorefrontService.BuiltOrderItems built =
                storefrontService.buildOrderItemsFromRequest(shop, request.getItems());
        List<OrderItem> newItems = built.items();

        Order openOrder = Boolean.TRUE.equals(table.getAlwaysAvailable())
                ? null
                : findCurrentOpenOrder(table);

        boolean created;
        Order saved;
        if (openOrder != null) {
            // Append vào đơn đang mở: cộng quantity nếu trùng productId+variantId+empty toppings.
            List<OrderItem> merged = mergeItems(openOrder.getItems(), newItems);
            openOrder.setItems(merged);
            double totalPrice = merged.stream()
                    .mapToDouble(line -> Math.max(1, line.getQuantity()) * line.getPriceAfterDiscount())
                    .sum();
            openOrder.setTotalPrice(totalPrice);
            openOrder.setTotalAmount(totalPrice);
            if (StringUtils.hasText(request.getCustomerNote())) {
                String existing = openOrder.getNote() == null ? "" : openOrder.getNote();
                String addition = "\nGhi chú thêm (" + LocalDateTime.now() + "): " + request.getCustomerNote().trim();
                openOrder.setNote(existing + addition);
            }
            saved = orderRepository.save(openOrder);
            created = false;
            publishOrderEvent(shop, saved, WebSocketMessageType.ORDER_UPDATED);
        } else {
            String branchId = StringUtils.hasText(table.getBranchId())
                    ? table.getBranchId()
                    : storefrontService.resolveDefaultBranchIdFor(shop.getId());

            Order order = new Order();
            order.setShopId(shop.getId());
            order.setBranchId(branchId);
            order.setTableId(table.getId());
            order.setOrderSource(OrderSource.IN_STORE);
            order.setStatus(OrderStatus.PENDING);
            order.setItems(newItems);
            order.setTotalPrice(built.totalPrice());
            order.setTotalAmount(built.totalPrice());
            order.setPaymentMethod("CASH");
            order.setShippingMethod("DINE_IN");
            order.setPaid(false);
            order.setPaymentStatus(PaymentStatus.PENDING_COLLECTION);
            if (StringUtils.hasText(request.getCustomerName())) {
                order.setGuestName(request.getCustomerName().trim());
            }
            if (StringUtils.hasText(request.getCustomerNote())) {
                order.setNote("[Đơn tại bàn " + table.getName() + "]\n"
                        + "Ghi chú: " + request.getCustomerNote().trim());
            } else {
                order.setNote("[Đơn tại bàn " + table.getName() + "]");
            }
            order.setOrderCode(storefrontService.generateOrderCodeFor(shop.getId()));
            saved = orderRepository.save(order);

            if (!Boolean.TRUE.equals(table.getAlwaysAvailable())) {
                table.setCurrentOrderId(saved.getId());
                table.setStatus(TableStatus.OCCUPIED);
                Table savedTable = tableRepository.save(table);
                publishTableStatus(savedTable);
            }
            created = true;
            publishOrderEvent(shop, saved, WebSocketMessageType.ORDER_CREATED);
            notifyOwnerSafely(shop, saved, table);
        }

        return new CreatedOrAppended(toOrderResponse(saved), created);
    }

    // ---------- internal helpers ----------

    private Order findCurrentOpenOrder(Table table) {
        if (Boolean.TRUE.equals(table.getAlwaysAvailable())) return null;
        String currentId = table.getCurrentOrderId();
        if (!StringUtils.hasText(currentId)) return null;
        Order order = orderRepository.findByIdAndDeletedFalse(currentId).orElse(null);
        if (order == null) {
            // self-heal: pointer rác → clear để lần sau tạo mới
            clearCurrentOrderPointer(table, currentId);
            return null;
        }
        if (order.getOrderSource() != OrderSource.IN_STORE
                || order.isPaid()
                || CLOSED_STATUSES.contains(order.getStatus())) {
            clearCurrentOrderPointer(table, currentId);
            return null;
        }
        return order;
    }

    private void clearCurrentOrderPointer(Table table, String expectedId) {
        if (!Objects.equals(table.getCurrentOrderId(), expectedId)) return;
        table.setCurrentOrderId(null);
        table.setStatus(TableStatus.AVAILABLE);
        Table saved = tableRepository.save(table);
        publishTableStatus(saved);
    }

    /**
     * Gộp item mới vào danh sách cũ — match theo {@code productId + variantId} (toppings null).
     * Cộng dồn quantity thay vì thêm dòng mới để đơn nhìn gọn.
     */
    private List<OrderItem> mergeItems(List<OrderItem> existing, List<OrderItem> additions) {
        List<OrderItem> result = new ArrayList<>();
        Map<String, OrderItem> index = new LinkedHashMap<>();
        if (existing != null) {
            for (OrderItem it : existing) {
                String key = mergeKey(it);
                if (key != null && it.getToppings() == null) {
                    index.put(key, it);
                }
                result.add(it);
            }
        }
        for (OrderItem add : additions) {
            String key = mergeKey(add);
            OrderItem found = key == null ? null : index.get(key);
            if (found != null) {
                found.setQuantity(found.getQuantity() + Math.max(1, add.getQuantity()));
            } else {
                result.add(add);
                if (key != null) index.put(key, add);
            }
        }
        return result;
    }

    private String mergeKey(OrderItem it) {
        if (it.getProductId() == null) return null;
        return it.getProductId() + "::" + (it.getVariantId() == null ? "" : it.getVariantId());
    }

    private void publishOrderEvent(Shop shop, Order order, WebSocketMessageType type) {
        if (order.getBranchId() == null) return;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.getId());
        payload.put("orderCode", order.getOrderCode());
        payload.put("shopId", shop.getId());
        payload.put("branchId", order.getBranchId());
        payload.put("tableId", order.getTableId());
        payload.put("totalAmount", order.getTotalAmount());
        payload.put("status", order.getStatus() != null ? order.getStatus().name() : null);
        payload.put("orderSource", OrderSource.IN_STORE.name());
        realtimeEventPublisher.publishOrderEvent(shop.getId(), order.getBranchId(), type, payload);
    }

    private void publishTableStatus(Table table) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tableId", table.getId());
        payload.put("status", table.getStatus() != null ? table.getStatus().name() : null);
        payload.put("currentOrderId", table.getCurrentOrderId());
        realtimeEventPublisher.publishTableEvent(table.getShopId(), table.getBranchId(),
                WebSocketMessageType.TABLE_STATUS_CHANGED, payload);
    }

    private void notifyOwnerSafely(Shop shop, Order order, Table table) {
        try {
            onlineOrderNotifier.notifyInStoreOrderCreated(shop, order, table);
        } catch (Exception ex) {
            log.warn("Failed to notify owner for in-store order {} (table {}): {}",
                    order.getOrderCode(), table.getId(), ex.getMessage());
        }
    }

    private com.example.sales.dto.storefront.StorefrontShopResponse buildShopResponse(Shop shop) {
        return com.example.sales.dto.storefront.StorefrontShopResponse.builder()
                .id(shop.getId())
                .name(shop.getName())
                .slug(shop.getSlug())
                .logoUrl(shop.getLogoUrl())
                .address(shop.getAddress())
                .phone(shop.getPhone())
                .currency(shop.getCurrency())
                .zaloPageUrl(shop.getZaloPageUrl())
                .facebookUrl(shop.getFacebookUrl())
                .tiktokUrl(shop.getTiktokUrl())
                .shopeeUrl(shop.getShopeeUrl())
                .build();
    }

    private TableOrderResponse toOrderResponse(Order order) {
        List<TableOrderResponse.TableOrderLine> lines = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem it : order.getItems()) {
                int qty = Math.max(1, it.getQuantity());
                double unit = it.getPriceAfterDiscount() > 0 ? it.getPriceAfterDiscount() : it.getPrice();
                lines.add(TableOrderResponse.TableOrderLine.builder()
                        .productId(it.getProductId())
                        .productName(it.getProductName())
                        .variantName(it.getVariantName())
                        .quantity(qty)
                        .unitPrice(unit)
                        .lineTotal(unit * qty)
                        .build());
            }
        }
        return TableOrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .paymentMethod(order.getPaymentMethod())
                .paid(order.isPaid())
                .totalPrice(order.getTotalPrice())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .customerNote(order.getNote())
                .items(lines)
                .build();
    }

    /** Bundle nội bộ cho {@link #resolve(String)}. */
    public record ResolvedTable(Shop shop, Table table, Branch branch) {}

    /** Bundle trả về cho controller — biết được tạo mới hay append. */
    public record CreatedOrAppended(TableOrderResponse order, boolean created) {}
}
