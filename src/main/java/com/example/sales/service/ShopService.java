// File: src/main/java/com/example/sales/service/ShopService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.constant.UserRole;
import com.example.sales.dto.shop.ShopAdminResponse;
import com.example.sales.dto.shop.ShopRequest;
import com.example.sales.dto.shop.ShopResponse;
import com.example.sales.dto.shop.ShopSimpleResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Branch;
import com.example.sales.model.Shop;
import com.example.sales.model.ShopUser;
import com.example.sales.repository.BranchRepository;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.ShopUserRepository;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.PermissionUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShopService extends BaseService {

    private final ShopRepository shopRepository;
    private final AuditLogService auditLogService;
    private final ShopUserRepository shopUserRepository;
    private final BranchRepository branchRepository; // Thêm BranchRepository

    public Shop createShop(String userId, ShopRequest request, String logoUrl) {
        if (shopRepository.existsByNameAndDeletedFalse(request.getName())) {
            throw new BusinessException(ApiCode.SHOP_NAME_EXISTS);
        }
        if (shopRepository.findByOwnerIdAndDeletedFalse(userId).isPresent()) {
            throw new BusinessException(ApiCode.SHOP_ALREADY_EXISTS);
        }

        Shop shop = new Shop();
        shop.setName(request.getName());
        shop.setType(request.getType());
        shop.setAddress(request.getAddress());
        shop.setPhone(request.getPhone());
        shop.setLogoUrl(logoUrl);
        shop.setOwnerId(userId);

        Shop savedShop = shopRepository.save(shop); // Đổi tên biến để rõ ràng hơn

        // ✅ Tạo chi nhánh mặc định
        Branch defaultBranch = Branch.builder()
                .shopId(savedShop.getId())
                .name("Chi nhánh chính") // Tên chi nhánh mặc định
                .address(request.getAddress()) // Lấy địa chỉ từ request của shop
                .phone(request.getPhone()) // Lấy số điện thoại từ request của shop
                .build();
        Branch savedBranch = branchRepository.save(defaultBranch); // Lưu chi nhánh

        // ✅ Cập nhật ShopUser để liên kết với chi nhánh mặc định
        ShopUser shopUser = ShopUser.builder()
                .shopId(savedShop.getId())
                .userId(userId)
                .role(ShopRole.OWNER)
                .branchId(savedBranch.getId())
                .permissions(PermissionUtils.getDefaultPermissions(ShopRole.OWNER))
                .build();
        shopUserRepository.save(shopUser);

        auditLogService.log(userId, savedShop.getId(), savedShop.getId(), "SHOP", "CREATED",
                String.format("Tạo cửa hàng: %s (%s)", savedShop.getName(), savedShop.getType()));

        return savedShop;
    }

    @Cacheable(value = "shops", key = "#ownerId")
    public Shop getShopByOwner(String ownerId) {
        return shopRepository.findByOwnerIdAndDeletedFalse(ownerId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
    }

    public Shop updateShop(String ownerId, ShopRequest request) {
        Shop shop = getShopByOwner(ownerId);

        shop.setName(request.getName());
        shop.setType(request.getType());
        shop.setAddress(request.getAddress());
        shop.setPhone(request.getPhone());

        Shop saved = shopRepository.save(shop);
        auditLogService.log(null, saved.getId(), saved.getId(), "SHOP", "UPDATED",
                String.format("Cập nhật cửa hàng: %s (%s)", saved.getName(), saved.getType()));
        return saved;
    }

    public void deleteShop(String ownerId) {
        Shop shop = getShopByOwner(ownerId);
        shop.setDeleted(true);
        shopRepository.save(shop);
        auditLogService.log(null, shop.getId(), shop.getId(), "SHOP", "DELETED",
                String.format("Xoá mềm cửa hàng: %s", shop.getName()));
    }

    @Cacheable(value = "shops", key = "#shopId")
    public Shop getShopById(String shopId) {
        return checkShopExists(shopRepository, shopId);
    }

    // File: ShopService.java
    public Page<ShopSimpleResponse> searchShops(String keyword, Pageable pageable) {
        return shopRepository.findByKeyword(keyword, pageable)
                .map(shop -> ShopSimpleResponse.builder()
                        .id(shop.getId())
                        .name(shop.getName())
                        .type(shop.getType())
                        .logoUrl(shop.getLogoUrl())
                        .active(shop.isActive())
                        .build());
    }

    public Shop save(Shop shop) {
        return shopRepository.save(shop);
    }

    public Object getShopResponse(CustomUserDetails user, Shop shop) {
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            return ShopAdminResponse.builder()
                    .id(shop.getId())
                    .name(shop.getName())
                    .type(shop.getType())
                    .address(shop.getAddress())
                    .phone(shop.getPhone())
                    .logoUrl(shop.getLogoUrl())
                    .active(shop.isActive())
                    .plan(shop.getPlan())
                    .currency(shop.getCurrency())
                    .timezone(shop.getTimezone())
                    .orderPrefix(shop.getOrderPrefix())
                    .planExpiry(shop.getPlanExpiry())
                    .build();
        } else {
            return ShopResponse.builder()
                    .id(shop.getId())
                    .name(shop.getName())
                    .type(shop.getType())
                    .address(shop.getAddress())
                    .phone(shop.getPhone())
                    .logoUrl(shop.getLogoUrl())
                    .active(shop.isActive())
                    .plan(shop.getPlan())
                    .currency(shop.getCurrency())
                    .build();
        }
    }
}