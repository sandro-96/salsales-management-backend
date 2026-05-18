package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.user.UserOrderHistoryItem;
import com.example.sales.dto.user.UserOrderHistoryLineItem;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Order;
import com.example.sales.model.OrderItem;
import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.repository.OrderRepository;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.UserRepository;
import com.example.sales.util.OnlineOrderNoteParser;
import com.example.sales.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserOrderHistoryService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;

    public Page<UserOrderHistoryItem> getStorefrontOrderHistory(String userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));

        String normalized = resolveUserPhoneNormalized(user);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ApiCode.USER_PHONE_REQUIRED);
        }

        List<String> variants = PhoneUtils.matchVariants(normalized);
        Page<Order> orders = orderRepository.findOnlineOrdersByPhoneMatch(normalized, variants, pageable);

        Set<String> shopIds = orders.getContent().stream()
                .map(Order::getShopId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, Shop> shopsById = shopRepository.findAllById(shopIds).stream()
                .collect(Collectors.toMap(Shop::getId, s -> s, (a, b) -> a));

        return orders.map(order -> toItem(order, shopsById.get(order.getShopId())));
    }

    private String resolveUserPhoneNormalized(User user) {
        if (StringUtils.hasText(user.getPhoneNormalized())) {
            return user.getPhoneNormalized();
        }
        String fromPhone = PhoneUtils.normalizeForMatch(user.getPhone());
        if (StringUtils.hasText(fromPhone)) {
            user.setPhoneNormalized(fromPhone);
            userRepository.save(user);
        }
        return fromPhone;
    }

    private static UserOrderHistoryItem toItem(Order order, Shop shop) {
        List<OrderItem> lines = order.getItems() != null ? order.getItems() : Collections.emptyList();
        int itemCount = lines.size();
        OnlineOrderNoteParser.ParsedOnlineNote parsed =
                OnlineOrderNoteParser.parse(order.getNote());

        return UserOrderHistoryItem.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .shopId(order.getShopId())
                .shopName(shop != null ? shop.getName() : null)
                .shopSlug(shop != null ? shop.getSlug() : null)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .paymentMethod(order.getPaymentMethod())
                .createdAt(order.getCreatedAt())
                .itemCount(itemCount)
                .customerName(order.getGuestName())
                .customerPhone(order.getGuestPhone())
                .customerEmail(parsed.customerEmail())
                .shippingAddress(parsed.shippingAddress())
                .customerNote(parsed.customerNote())
                .items(lines.stream().map(UserOrderHistoryService::toLineItem).toList())
                .build();
    }

    private static UserOrderHistoryLineItem toLineItem(OrderItem line) {
        int qty = Math.max(1, line.getQuantity());
        double unit = line.getPriceAfterDiscount() > 0
                ? line.getPriceAfterDiscount()
                : line.getPrice();
        return UserOrderHistoryLineItem.builder()
                .productName(line.getProductName())
                .variantName(line.getVariantName())
                .sku(line.getSku())
                .quantity(qty)
                .unitPrice(unit)
                .lineTotal(unit * qty)
                .build();
    }
}
