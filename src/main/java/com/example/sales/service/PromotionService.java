// File: src/main/java/com/example/sales/service/PromotionService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.promotion.PromotionRequest;
import com.example.sales.dto.promotion.PromotionResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Promotion;
import com.example.sales.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final AuditLogService auditLogService;

    public List<PromotionResponse> getAll(String shopId, String branchId) {
        return promotionRepository.findByShopIdAndBranchIdAndDeletedFalse(shopId, branchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PromotionResponse create(String shopId, PromotionRequest request) {
        Promotion promotion = Promotion.builder()
                .shopId(shopId)
                .name(request.getName())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .applicableProductIds(request.getApplicableProductIds())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(request.isActive())
                .branchId(request.getBranchId())
                .build();

        Promotion saved = promotionRepository.save(promotion);
        auditLogService.log(null, shopId, saved.getId(), "PROMOTION", "CREATED",
                String.format("Tạo khuyến mãi: %s (%.2f %s)",
                        saved.getName(),
                        saved.getDiscountValue(),
                        saved.getDiscountType()));
        return toResponse(saved);
    }

    public PromotionResponse update(String shopId, String id, PromotionRequest request) {
        Promotion promotion = promotionRepository.findByIdAndDeletedFalse(id)
                .filter(p -> p.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PROMOTION_NOT_FOUND));

        if (!promotion.getBranchId().equals(request.getBranchId())) {
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }

        promotion.setName(request.getName());
        promotion.setDiscountType(request.getDiscountType());
        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setApplicableProductIds(request.getApplicableProductIds());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setActive(request.isActive());

        Promotion saved = promotionRepository.save(promotion);
        auditLogService.log(null, shopId, saved.getId(), "PROMOTION", "UPDATED",
                String.format("Cập nhật khuyến mãi: %s", saved.getName()));
        return toResponse(saved);
    }

    public void delete(String shopId, String id) {
        Promotion promotion = promotionRepository.findByIdAndDeletedFalse(id)
                .filter(p -> p.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.PROMOTION_NOT_FOUND));

        promotion.setDeleted(true);
        promotionRepository.save(promotion);
        auditLogService.log(null, shopId, promotion.getId(), "PROMOTION", "DELETED",
                String.format("Xoá mềm khuyến mãi: %s", promotion.getName()));

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
}