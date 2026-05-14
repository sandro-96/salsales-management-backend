// File: src/main/java/com/example/sales/service/storefront/StorefrontService.java
package com.example.sales.service.storefront;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.AppConstants;
import com.example.sales.constant.OrderSource;
import com.example.sales.constant.OrderStatus;
import com.example.sales.constant.PaymentStatus;
import com.example.sales.dto.storefront.StorefrontBranchResponse;
import com.example.sales.dto.storefront.StorefrontOrderItemRequest;
import com.example.sales.dto.storefront.StorefrontOrderRequest;
import com.example.sales.dto.storefront.StorefrontOrderResponse;
import com.example.sales.dto.storefront.StorefrontProductResponse;
import com.example.sales.dto.storefront.StorefrontProductVariantResponse;
import com.example.sales.dto.storefront.StorefrontShopResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Branch;
import com.example.sales.model.Order;
import com.example.sales.model.OrderItem;
import com.example.sales.model.Product;
import com.example.sales.model.ProductVariant;
import com.example.sales.model.Shop;
import com.example.sales.repository.BranchRepository;
import com.example.sales.repository.OrderRepository;
import com.example.sales.repository.ProductRepository;
import com.example.sales.repository.ShopRepository;
import com.example.sales.service.SequenceService;
import com.example.sales.service.notification.OnlineOrderNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic cho storefront công khai (guest, không cần auth).
 * - Chỉ trả dữ liệu của shop có {@code onlineSalesEnabled = true && active = true}.
 * - Dùng {@code Product.defaultPrice} (hoặc giá variant) — không kiểm tồn kho per-branch.
 * - Đơn tạo ra luôn có {@code orderSource = ONLINE}, COD, status PENDING.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorefrontService {

    private static final DateTimeFormatter ORDER_CODE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final BranchRepository branchRepository;
    private final SequenceService sequenceService;
    private final OnlineOrderNotifier onlineOrderNotifier;

    /**
     * Lấy shop public theo slug. Ném {@link ResourceNotFoundException} nếu shop
     * không tồn tại / không active / chưa bật bán online.
     */
    public StorefrontShopResponse getShopBySlug(String slug) {
        Shop shop = findPublicShopOrThrow(slug);
        return toShopResponse(shop);
    }

    public List<String> listCategories(String slug) {
        Shop shop = findPublicShopOrThrow(slug);
        List<Product> all = productRepository.findAllByShopIdAndDeletedFalse(shop.getId());
        return all.stream()
                .filter(Product::isActive)
                .map(Product::getCategory)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public Page<StorefrontProductResponse> listProducts(String slug,
                                                        String keyword,
                                                        String category,
                                                        Pageable pageable) {
        Shop shop = findPublicShopOrThrow(slug);

        // Lấy toàn bộ active product của shop rồi lọc/paginate trong memory.
        // Bộ dữ liệu storefront mỗi shop không quá lớn ở MVP; có thể tối ưu bằng query Mongo sau.
        List<Product> all = productRepository.findAllByShopIdAndDeletedFalse(shop.getId()).stream()
                .filter(Product::isActive)
                .collect(Collectors.toList());

        String q = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        String cat = StringUtils.hasText(category) ? category.trim() : null;

        List<Product> filtered = all.stream()
                .filter(p -> cat == null || cat.equalsIgnoreCase(p.getCategory()))
                .filter(p -> q == null
                        || (p.getName() != null && p.getName().toLowerCase().contains(q))
                        || (p.getCategory() != null && p.getCategory().toLowerCase().contains(q)))
                .sorted((a, b) -> safe(a.getName()).compareToIgnoreCase(safe(b.getName())))
                .collect(Collectors.toList());

        Pageable page = pageable == null
                ? PageRequest.of(0, 24, Sort.by(Sort.Direction.ASC, "name"))
                : pageable;
        int start = (int) page.getOffset();
        int end = Math.min(start + page.getPageSize(), filtered.size());
        List<StorefrontProductResponse> content = start >= filtered.size()
                ? Collections.emptyList()
                : filtered.subList(start, end).stream()
                        .map(this::toProductResponse)
                        .collect(Collectors.toList());
        return new PageImpl<>(content, page, filtered.size());
    }

    public StorefrontProductResponse getProductDetail(String slug, String productId) {
        Shop shop = findPublicShopOrThrow(slug);
        Product product = productRepository.findByIdAndShopIdAndDeletedFalse(productId, shop.getId())
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));
        return toProductResponse(product);
    }

    @Transactional
    public StorefrontOrderResponse createOrder(String slug, StorefrontOrderRequest request) {
        Shop shop = findPublicShopOrThrow(slug);

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(ApiCode.STOREFRONT_EMPTY_CART);
        }

        // Tải tất cả product được tham chiếu (chỉ active, đúng shop)
        Set<String> productIds = request.getItems().stream()
                .map(StorefrontOrderItemRequest::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        if (productIds.isEmpty()) {
            throw new BusinessException(ApiCode.STOREFRONT_EMPTY_CART);
        }

        List<OrderItem> orderItems = new ArrayList<>();
        double totalPrice = 0.0;

        for (StorefrontOrderItemRequest line : request.getItems()) {
            Product product = productRepository.findByIdAndShopIdAndDeletedFalse(line.getProductId(), shop.getId())
                    .filter(Product::isActive)
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PRODUCT_NOT_FOUND));

            double linePrice;
            String variantName = null;
            String variantId = null;
            String sku = product.getSku();
            boolean hasVariants = product.getVariants() != null && !product.getVariants().isEmpty();

            if (hasVariants) {
                if (!StringUtils.hasText(line.getVariantId())) {
                    throw new BusinessException(ApiCode.ORDER_LINE_VARIANT_REQUIRED);
                }
                ProductVariant variant = product.getVariants().stream()
                        .filter(v -> line.getVariantId().equals(v.getVariantId()))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(ApiCode.PRODUCT_VARIANT_NOT_FOUND));
                linePrice = variant.getPrice() > 0 ? variant.getPrice() : product.getDefaultPrice();
                variantName = variant.getName();
                variantId = variant.getVariantId();
                if (StringUtils.hasText(variant.getSku())) {
                    sku = variant.getSku();
                }
            } else {
                if (StringUtils.hasText(line.getVariantId())) {
                    throw new BusinessException(ApiCode.ORDER_LINE_VARIANT_NOT_ALLOWED);
                }
                linePrice = product.getDefaultPrice();
            }

            int qty = Math.max(1, line.getQuantity());
            totalPrice += linePrice * qty;

            orderItems.add(OrderItem.builder()
                    .productId(product.getId())
                    .variantId(variantId)
                    .productName(product.getName())
                    .variantName(variantName)
                    .sku(sku)
                    .quantity(qty)
                    .price(linePrice)
                    .priceAfterDiscount(linePrice)
                    .trackInventory(false) // bỏ qua tồn kho cho MVP online
                    .build());
        }

        // Tìm chi nhánh mặc định để gắn đơn (Order yêu cầu branchId)
        String defaultBranchId = resolveDefaultBranchId(shop.getId());

        Order order = new Order();
        order.setShopId(shop.getId());
        order.setBranchId(defaultBranchId);
        order.setOrderSource(OrderSource.ONLINE);
        order.setStatus(OrderStatus.PENDING);
        order.setItems(orderItems);
        order.setTotalPrice(totalPrice);
        order.setTotalAmount(totalPrice); // chưa có thuế / shipping fee ở MVP
        order.setPaymentMethod("COD");
        order.setShippingMethod("COD");
        order.setPaid(false);
        order.setPaymentStatus(PaymentStatus.PENDING_COLLECTION);
        order.setGuestName(request.getCustomerName().trim());
        order.setGuestPhone(request.getCustomerPhone().trim());
        order.setNote(composeShippingNote(request));
        order.setOrderCode(generateOrderCode(shop.getId()));

        Order saved = orderRepository.save(order);

        // Thông báo (best-effort: WebSocket + email)
        notifyOwnerSafely(shop, saved);

        return StorefrontOrderResponse.builder()
                .id(saved.getId())
                .orderCode(saved.getOrderCode())
                .customerName(saved.getGuestName())
                .customerPhone(saved.getGuestPhone())
                .totalAmount(saved.getTotalAmount())
                .paymentMethod(saved.getPaymentMethod())
                .status(saved.getStatus() != null ? saved.getStatus().name() : null)
                .build();
    }

    // ---------- Internal helpers ----------

    private Shop findPublicShopOrThrow(String slug) {
        if (!StringUtils.hasText(slug)) {
            throw new ResourceNotFoundException(ApiCode.SHOP_NOT_FOUND);
        }
        Shop shop = shopRepository.findBySlugAndDeletedFalse(slug.trim())
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.SHOP_NOT_FOUND));
        if (!shop.isActive() || !shop.isOnlineSalesEnabled()) {
            // Trả mã 4180 ONLINE_SALES_DISABLED (BusinessException giữ nguyên mã code)
            // để FE phân biệt với "shop không tồn tại" và hiển thị thông báo riêng.
            throw new BusinessException(ApiCode.ONLINE_SALES_DISABLED);
        }
        return shop;
    }

    private String resolveDefaultBranchId(String shopId) {
        List<Branch> branches = branchRepository.findAllByShopIdAndDeletedFalse(shopId);
        if (branches.isEmpty()) {
            // Shop hợp lệ thì phải có ít nhất 1 chi nhánh — fail-safe theo BusinessException
            throw new BusinessException(ApiCode.BRANCH_NOT_FOUND);
        }
        return branches.stream()
                .filter(Branch::isDefault)
                .findFirst()
                .orElse(branches.get(0))
                .getId();
    }

    private String generateOrderCode(String shopId) {
        String raw = sequenceService.getNextCode(shopId, "DH", AppConstants.SequenceTypes.SEQUENCE_TYPE_ORDER);
        sequenceService.updateNextSequence(shopId, "DH", AppConstants.SequenceTypes.SEQUENCE_TYPE_ORDER);
        String tail = raw.contains("_") ? raw.substring(raw.lastIndexOf('_') + 1) : raw;
        return "DH-" + LocalDate.now().format(ORDER_CODE_DATE) + "-" + tail;
    }

    private String composeShippingNote(StorefrontOrderRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Đơn online]\n");
        sb.append("Địa chỉ: ").append(safe(req.getAddressLine()));
        if (StringUtils.hasText(req.getWard())) sb.append(", ").append(req.getWard().trim());
        if (StringUtils.hasText(req.getDistrict())) sb.append(", ").append(req.getDistrict().trim());
        if (StringUtils.hasText(req.getProvince())) sb.append(", ").append(req.getProvince().trim());
        sb.append('\n');
        if (StringUtils.hasText(req.getCustomerEmail())) {
            sb.append("Email: ").append(req.getCustomerEmail().trim()).append('\n');
        }
        if (StringUtils.hasText(req.getNote())) {
            sb.append("Ghi chú: ").append(req.getNote().trim()).append('\n');
        }
        return sb.toString().trim();
    }

    private void notifyOwnerSafely(Shop shop, Order order) {
        try {
            onlineOrderNotifier.notifyOnlineOrderCreated(shop, order);
        } catch (Exception ex) {
            // best-effort
            log.warn("Failed to notify owner for online order {} of shop {}: {}",
                    order.getOrderCode(), shop.getId(), ex.getMessage());
        }
    }

    private StorefrontShopResponse toShopResponse(Shop shop) {
        return StorefrontShopResponse.builder()
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
                .branches(loadPublicBranches(shop.getId()))
                .build();
    }

    private List<StorefrontBranchResponse> loadPublicBranches(String shopId) {
        return branchRepository.findAllByShopIdAndDeletedFalse(shopId).stream()
                .filter(Branch::isActive)
                .sorted((a, b) -> {
                    // Chi nhánh mặc định lên đầu, sau đó sort theo tên.
                    if (a.isDefault() != b.isDefault()) return a.isDefault() ? -1 : 1;
                    return safe(a.getName()).compareToIgnoreCase(safe(b.getName()));
                })
                .map(b -> StorefrontBranchResponse.builder()
                        .id(b.getId())
                        .name(b.getName())
                        .address(b.getAddress())
                        .phone(b.getPhone())
                        .openingTime(b.getOpeningTime())
                        .closingTime(b.getClosingTime())
                        .isDefault(b.isDefault())
                        .build())
                .collect(Collectors.toList());
    }

    private StorefrontProductResponse toProductResponse(Product product) {
        List<StorefrontProductVariantResponse> variants = null;
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            variants = product.getVariants().stream()
                    .map(v -> StorefrontProductVariantResponse.builder()
                            .variantId(v.getVariantId())
                            .name(v.getName())
                            .price(v.getPrice() > 0 ? v.getPrice() : product.getDefaultPrice())
                            .images(v.getImages())
                            .build())
                    .collect(Collectors.toList());
        }
        return StorefrontProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .description(product.getDescription())
                .unit(product.getUnit())
                .price(product.getDefaultPrice())
                .images(product.getImages())
                .variants(variants)
                .build();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
