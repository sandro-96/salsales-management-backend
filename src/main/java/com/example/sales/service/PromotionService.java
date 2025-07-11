// File: src/main/java/com/example/sales/service/PromotionService.java
package com.example.sales.service;

import com.example.sales.constant.ApiErrorCode;
import com.example.sales.dto.promotion.PromotionRequest;
import com.example.sales.dto.promotion.PromotionResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Promotion;
import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.repository.PromotionRepository;
import com.example.sales.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final ShopRepository shopRepository;

    public List<PromotionResponse> getAll(User user, String branchId) {
        String shopId = getShopOfUser(user).getId();
        return promotionRepository.findByShopIdAndBranchId(shopId, branchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PromotionResponse create(User user, PromotionRequest request) {
        Shop shop = getShopOfUser(user);
        Promotion promotion = Promotion.builder()
                .shopId(shop.getId())
                .name(request.getName())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .applicableProductIds(request.getApplicableProductIds())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(request.isActive())
                .branchId(request.getBranchId())
                .build();

        return toResponse(promotionRepository.save(promotion));
    }

    public PromotionResponse update(User user, String id, PromotionRequest request) {
        String shopId = getShopOfUser(user).getId();

        Promotion promotion = promotionRepository.findById(id)
                .filter(p -> p.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.PROMOTION_NOT_FOUND));
        if (!promotion.getBranchId().equals(request.getBranchId())) {
            throw new BusinessException(ApiErrorCode.UNAUTHORIZED);
        }

        promotion.setName(request.getName());
        promotion.setDiscountType(request.getDiscountType());
        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setApplicableProductIds(request.getApplicableProductIds());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setActive(request.isActive());

        return toResponse(promotionRepository.save(promotion));
    }

    public void delete(User user, String id) {
        String shopId = getShopOfUser(user).getId();
        Promotion promotion = promotionRepository.findById(id)
                .filter(p -> p.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.PROMOTION_NOT_FOUND));

        promotionRepository.delete(promotion);
    }

    private PromotionResponse toResponse(Promotion p) {
        return PromotionResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .discountType(p.getDiscountType())
                .discountValue(p.getDiscountValue())
                .applicableProductIds(p.getApplicableProductIds())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .active(p.isActive())
                .build();
    }

    private Shop getShopOfUser(User user) {
        return shopRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.SHOP_NOT_FOUND));
    }
}
