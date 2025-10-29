// File: src/main/java/com/example/sales/service/ShopUserService.java
package com.example.sales.service;

import com.example.sales.cache.ShopUserCache;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.shop.ShopSimpleResponse;
import com.example.sales.dto.shopUser.ShopMemberResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Shop;
import com.example.sales.model.ShopUser;
import com.example.sales.model.User;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.ShopUserRepository;
import com.example.sales.repository.UserRepository;
import com.example.sales.security.PermissionUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopUserService extends BaseService {

    private final ShopUserRepository shopUserRepository;
    private final ShopRepository shopRepository;
    private final AuditLogService auditLogService;
    private final ShopUserCache shopUserCache;
    private final UserRepository userRepository;

    public void requireAnyRole(String shopId, String userId, ShopRole... roles) {
        ShopRole actual = shopUserCache.getUserRoleInShop(shopId, userId);
        if (Arrays.stream(roles).noneMatch(role -> role == actual)) {
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }
    }

    public void addUser(String shopId, String userId, ShopRole role, String performedByUserId) {
        validateRoleHierarchy(shopId, performedByUserId, userId);
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        if (!shop.isActive()) {
            throw new BusinessException(ApiCode.SHOP_INACTIVE);
        }

        Optional<ShopUser> existingShopUser = shopUserRepository.findByShopIdAndUserId(shopId, userId);

        if (existingShopUser.isPresent()) {
            ShopUser shopUser = existingShopUser.get();
            if (!shopUser.isDeleted()) {
                throw new BusinessException(ApiCode.USER_ALREADY_IN_SHOP);
            } else {
                shopUser.setDeleted(false);
                shopUser.setRole(role);
                shopUser.setPermissions(PermissionUtils.getDefaultPermissions(role));
                shopUserRepository.save(shopUser);
                auditLogService.log(performedByUserId, shopId, shopUser.getId(), "SHOP_USER", "RESTORED",
                        String.format("Khôi phục người dùng %s của cửa hàng %s với vai trò %s", userId, shopId, role));
            }
        } else {
            ShopUser newShopUser = ShopUser.builder()
                    .shopId(shopId)
                    .userId(userId)
                    .role(role)
                    .permissions(PermissionUtils.getDefaultPermissions(role))
                    .build();
            shopUserRepository.save(newShopUser);
            auditLogService.log(performedByUserId, shopId, newShopUser.getId(), "SHOP_USER", "ADDED",
                    String.format("Thêm người dùng %s vào cửa hàng %s với vai trò %s", userId, shopId, role));
        }
    }

    public void removeUser(String shopId, String userId, String performedByUserId) {
        validateRoleHierarchy(shopId, performedByUserId, userId);
        if (userId.equals(performedByUserId)) {
            auditLogService.log(performedByUserId, shopId, userId, "SHOP_USER", "UNAUTHORIZED",
                    "Người dùng không thể xóa chính mình khỏi cửa hàng");
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }
        ShopUser shopUser = shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, userId)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));
        shopUser.setDeleted(true);
        shopUserRepository.save(shopUser);

        auditLogService.log(performedByUserId, shopId, shopUser.getId(), "SHOP_USER", "REMOVED",
                String.format("Xoá mềm người dùng %s khỏi cửa hàng %s", userId, shopId));
    }

    public Page<ShopSimpleResponse> getShopsForUser(String userId, Pageable pageable) {
        Sort sort = pageable.getSort().and(Sort.by("createdAt").descending());
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<ShopUser> shopUsers = shopUserRepository.findByUserIdAndDeletedFalse(userId, pageable);
        List<String> shopIds = shopUsers.getContent().stream()
                .map(ShopUser::getShopId)
                .toList();

        List<Shop> shops = shopRepository.findByIdInAndDeletedFalse(shopIds);
        Map<String, Shop> shopMap = shops.stream()
                .collect(Collectors.toMap(Shop::getId, Function.identity()));

        List<ShopSimpleResponse> shopResponses = shopUsers.getContent().stream()
                .map(su -> shopMap.get(su.getShopId()))
                .filter(Objects::nonNull)
                .map(shop -> {
                    ShopUser su = shopUsers.getContent().stream()
                            .filter(u -> Objects.equals(u.getShopId(), shop.getId()))
                            .findFirst()
                            .orElse(null);

                    return ShopSimpleResponse.builder()
                            .id(shop.getId())
                            .name(shop.getName())
                            .type(shop.getType())
                            .logoUrl(shop.getLogoUrl())
                            .countryCode(shop.getCountryCode())
                            .address(shop.getAddress())
                            .phone(shop.getPhone())
                            .active(shop.isActive())
                            .isTrackInventory(shop.isTrackInventory())
                            .role(su != null ? su.getRole() : null)
                            .industry(shop.getType().getIndustry())
                            .businessModel(shop.getBusinessModel())
                            .slug(shop.getSlug())
                            .build();
                })
                .toList();

        return new PageImpl<>(shopResponses, pageable, shopUsers.getTotalElements());
    }

    private void ensureCanModifyRole(ShopRole actorRole, ShopRole targetRole) {
        if (actorRole.ordinal() < targetRole.ordinal()) {
            auditLogService.log(null, null, null, "ROLE_MODIFICATION", "DENIED",
                    String.format("User with role %s attempted to modify user with role %s.", actorRole, targetRole));
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
    }

    public ShopRole getUserRole(String shopId, String userId) {
        return shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, userId)
                .map(ShopUser::getRole)
                .orElseThrow(() -> new BusinessException(ApiCode.UNAUTHORIZED));
    }

    public void validateRoleHierarchy(String shopId, String actorUserId, String targetUserId) {
        // Lấy role người thực hiện (actor) tại branch
        ShopRole actorRole = getUserRole(shopId, actorUserId);

        // Lấy role người bị thao tác (target)
        ShopRole targetRole = getUserRole(shopId, targetUserId);

        // Thực hiện logic so sánh
        ensureCanModifyRole(actorRole, targetRole);
    }

    public Page<ShopMemberResponse> getUsersInShop(String shopId, String userId, Pageable pageable) {
        // Kiểm tra cửa hàng có tồn tại và đang hoạt động
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        if (!shop.isActive()) {
            throw new BusinessException(ApiCode.SHOP_INACTIVE);
        }

        // Lấy danh sách các thành viên của cửa hàng
        Sort sort = pageable.getSort().and(Sort.by("createdAt").descending());
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Page<ShopUser> shopUsers = shopUserRepository.findByShopId(shopId, pageable);

        List<String> userIds = shopUsers.map(ShopUser::getUserId).toList();
        Map<String, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return shopUsers.map(su -> ShopMemberResponse.builder()
                .id(su.getId())
                .userId(su.getUserId())
                .fullName(userMap.getOrDefault(su.getUserId(), new User()).getFullName())
                .address(userMap.getOrDefault(su.getUserId(), new User()).getAddress())
                .email(userMap.getOrDefault(su.getUserId(), new User()).getEmail())
                .phone(userMap.getOrDefault(su.getUserId(), new User()).getPhone())
                .avatarUrl(userMap.getOrDefault(su.getUserId(), new User()).getAvatarUrl())
                .city(userMap.getOrDefault(su.getUserId(), new User()).getCity())
                .state(userMap.getOrDefault(su.getUserId(), new User()).getState())
                .birthDate(userMap.getOrDefault(su.getUserId(), new User()).getBirthDate())
                .createdAt(su.getCreatedAt())
                .gender(userMap.getOrDefault(su.getUserId(), new User()).getGender())
                .role(su.getRole())
                .build());
    }

    public void markDeletedByShopId(String shopId) {
        List<ShopUser> shopUsers = shopUserRepository.findByShopId(shopId, Pageable.unpaged()).getContent();
        for (ShopUser shopUser : shopUsers) {
            shopUser.setDeleted(true);
        }
        shopUserRepository.saveAll(shopUsers);
    }
}
