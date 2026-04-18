package com.example.sales.service;

import com.example.sales.cache.ProductCache;
import com.example.sales.cache.ShopCache;
import com.example.sales.constant.ApiCode;
import com.example.sales.dto.shop.ShopToppingRequest;
import com.example.sales.dto.shop.ShopToppingResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Shop;
import com.example.sales.model.ShopTopping;
import com.example.sales.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopToppingService extends BaseService {

    private final ShopRepository shopRepository;
    private final ShopCache shopCache;
    private final ProductCache productCache;
    private final AuditLogService auditLogService;

    public List<ShopToppingResponse> listToppings(String shopId) {
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        return toResponses(shop.getShopToppings());
    }

    /**
     * Thay toàn bộ danh mục topping của shop theo payload (giữ {@code toppingId} nếu client gửi lại).
     */
    public List<ShopToppingResponse> replaceToppings(String userId, String shopId, List<ShopToppingRequest> requests) {
        if (requests == null) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        Map<String, ShopTopping> seenIds = new LinkedHashMap<>();
        List<ShopTopping> built = new ArrayList<>();
        for (ShopToppingRequest req : requests) {
            if (req == null) {
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }
            if (!StringUtils.hasText(req.getName())) {
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }
            if (req.getExtraPrice() < 0) {
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }
            String tid = StringUtils.hasText(req.getToppingId()) ? req.getToppingId().trim() : UUID.randomUUID().toString();
            String key = tid.toLowerCase();
            if (seenIds.containsKey(key)) {
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }
            ShopTopping st = ShopTopping.builder()
                    .toppingId(tid)
                    .name(req.getName().trim())
                    .extraPrice(req.getExtraPrice())
                    .active(req.isActive())
                    .build();
            seenIds.put(key, st);
            built.add(st);
        }

        shop.setShopToppings(built.isEmpty() ? null : built);
        shopRepository.save(shop);
        shopCache.evictShopById(shopId);
        productCache.evictByShop(shopId);

        auditLogService.log(userId, shopId, shopId, "SHOP", "TOPPINGS_UPDATED",
                "Cập nhật danh mục topping shop (%d mục)".formatted(built.size()));

        return toResponses(shop.getShopToppings());
    }

    private static List<ShopToppingResponse> toResponses(List<ShopTopping> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream().map(st -> ShopToppingResponse.builder()
                .toppingId(st.getToppingId())
                .name(st.getName())
                .extraPrice(st.getExtraPrice())
                .active(st.isActive())
                .build()).toList();
    }
}
