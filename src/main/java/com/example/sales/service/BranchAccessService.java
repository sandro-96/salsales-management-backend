package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Order;
import com.example.sales.model.Shop;
import com.example.sales.model.ShopUser;
import com.example.sales.model.StaffProfile;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.ShopUserRepository;
import com.example.sales.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Giới hạn nhân viên STAFF/CASHIER theo {@link StaffProfile#branchId}.
 * OWNER, MANAGER và chủ shop được xem/thao tác mọi chi nhánh.
 */
@Service
@RequiredArgsConstructor
public class BranchAccessService {

    private final ShopRepository shopRepository;
    private final ShopUserRepository shopUserRepository;
    private final StaffProfileRepository staffProfileRepository;

    public boolean hasShopWideBranchAccess(String shopId, String userId) {
        if (!StringUtils.hasText(shopId) || !StringUtils.hasText(userId)) {
            return false;
        }
        Optional<Shop> shop = shopRepository.findByIdAndDeletedFalse(shopId);
        if (shop.isPresent() && userId.equals(shop.get().getOwnerId())) {
            return true;
        }
        return shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, userId)
                .map(ShopUser::getRole)
                .filter(role -> role == ShopRole.OWNER || role == ShopRole.MANAGER)
                .isPresent();
    }

    public Optional<String> getAssignedBranchId(String shopId, String userId) {
        if (!StringUtils.hasText(shopId) || !StringUtils.hasText(userId)) {
            return Optional.empty();
        }
        return staffProfileRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, userId)
                .map(StaffProfile::getBranchId)
                .filter(StringUtils::hasText);
    }

    public boolean isBranchRestricted(String shopId, String userId) {
        if (hasShopWideBranchAccess(shopId, userId)) {
            return false;
        }
        return getAssignedBranchId(shopId, userId).isPresent();
    }

    /**
     * Lọc chi nhánh khi gọi API: nhân viên gắn CN chỉ được dùng CN đó; null = tất cả CN (chỉ role rộng).
     */
    public String effectiveBranchFilter(String shopId, String userId, String requestedBranchId) {
        if (hasShopWideBranchAccess(shopId, userId)) {
            return StringUtils.hasText(requestedBranchId) ? requestedBranchId.trim() : null;
        }
        Optional<String> assigned = getAssignedBranchId(shopId, userId);
        if (assigned.isEmpty()) {
            return StringUtils.hasText(requestedBranchId) ? requestedBranchId.trim() : null;
        }
        String allowed = assigned.get();
        if (StringUtils.hasText(requestedBranchId)
                && !allowed.equals(requestedBranchId.trim())) {
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
        return allowed;
    }

    public void assertBranchAccess(String shopId, String userId, String branchId) {
        if (!StringUtils.hasText(branchId)) {
            if (isBranchRestricted(shopId, userId)) {
                throw new BusinessException(ApiCode.ACCESS_DENIED);
            }
            return;
        }
        effectiveBranchFilter(shopId, userId, branchId);
    }

    public void assertOrderBranchAccess(String shopId, String userId, Order order) {
        if (order == null) {
            return;
        }
        assertBranchAccess(shopId, userId, order.getBranchId());
    }
}
