// File: src/main/java/com/example/sales/service/ShopUserService.java
package com.example.sales.service;

import com.example.sales.cache.ShopUserCache;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.Permission;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.shop.ShopSimpleResponse;
import com.example.sales.dto.shopUser.ShopMemberResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Shop;
import com.example.sales.model.ShopUser;
import com.example.sales.model.StaffProfile;
import com.example.sales.model.User;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.ShopUserRepository;
import com.example.sales.repository.StaffProfileRepository;
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
    private final StaffProfileRepository staffProfileRepository;

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

    public ShopMemberResponse addUserByEmail(String shopId, String email, ShopRole role, String performedByUserId) {
        if (role == ShopRole.OWNER) {
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }

        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        if (!shop.isActive()) {
            throw new BusinessException(ApiCode.SHOP_INACTIVE);
        }

        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));

        Optional<ShopUser> existingShopUser = shopUserRepository.findByShopIdAndUserId(shopId, user.getId());

        ShopUser savedShopUser;
        if (existingShopUser.isPresent()) {
            ShopUser shopUser = existingShopUser.get();
            if (!shopUser.isDeleted()) {
                throw new BusinessException(ApiCode.USER_ALREADY_IN_SHOP);
            }
            shopUser.setDeleted(false);
            shopUser.setRole(role);
            shopUser.setPermissions(PermissionUtils.getDefaultPermissions(role));
            savedShopUser = shopUserRepository.save(shopUser);
            auditLogService.log(performedByUserId, shopId, shopUser.getId(), "SHOP_USER", "RESTORED",
                    String.format("Khôi phục người dùng %s vào cửa hàng %s với vai trò %s", email, shopId, role));
        } else {
            ShopUser newShopUser = ShopUser.builder()
                    .shopId(shopId)
                    .userId(user.getId())
                    .role(role)
                    .permissions(PermissionUtils.getDefaultPermissions(role))
                    .build();
            savedShopUser = shopUserRepository.save(newShopUser);
            auditLogService.log(performedByUserId, shopId, newShopUser.getId(), "SHOP_USER", "ADDED",
                    String.format("Thêm người dùng %s vào cửa hàng %s với vai trò %s", email, shopId, role));
        }

        return toMemberResponse(savedShopUser, user, null);
    }

    public void updateUserRole(String shopId, String targetUserId, ShopRole newRole, String performedByUserId) {
        if (newRole == ShopRole.OWNER) {
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
        if (targetUserId.equals(performedByUserId)) {
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }

        ShopUser target = shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, targetUserId)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));

        if (target.getRole() == ShopRole.OWNER) {
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }

        ShopRole actorRole = getUserRole(shopId, performedByUserId);
        ensureCanModifyRole(actorRole, target.getRole());

        ShopRole oldRole = target.getRole();
        target.setRole(newRole);
        target.setPermissions(PermissionUtils.getDefaultPermissions(newRole));
        shopUserRepository.save(target);

        auditLogService.log(performedByUserId, shopId, target.getId(), "SHOP_USER", "ROLE_UPDATED",
                String.format("Cập nhật vai trò từ %s thành %s cho người dùng %s", oldRole, newRole, targetUserId));
    }

    public void updatePermissions(String shopId, String targetUserId, Set<Permission> permissions, String performedByUserId) {
        if (targetUserId.equals(performedByUserId)) {
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }

        ShopUser target = shopUserRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, targetUserId)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));

        if (target.getRole() == ShopRole.OWNER) {
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }

        ShopRole actorRole = getUserRole(shopId, performedByUserId);
        ensureCanModifyRole(actorRole, target.getRole());

        target.setPermissions(permissions);
        shopUserRepository.save(target);

        auditLogService.log(performedByUserId, shopId, target.getId(), "SHOP_USER", "PERMISSIONS_UPDATED",
                String.format("Cập nhật quyền cho người dùng %s", targetUserId));
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

        if (shopUser.getRole() == ShopRole.OWNER) {
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }

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
                            .taxRegistrationNumber(shop.getTaxRegistrationNumber())
                            .active(shop.isActive())
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
        ShopRole actorRole = getUserRole(shopId, actorUserId);
        ShopRole targetRole = getUserRole(shopId, targetUserId);
        ensureCanModifyRole(actorRole, targetRole);
    }

    public Page<ShopMemberResponse> getUsersInShop(String shopId, String userId, String keyword, ShopRole roleFilter, Pageable pageable) {
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        if (!shop.isActive()) {
            throw new BusinessException(ApiCode.SHOP_INACTIVE);
        }

        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by("createdAt").descending();
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<ShopUser> shopUsers;
        if (roleFilter != null) {
            shopUsers = shopUserRepository.findByShopIdAndRoleAndDeletedFalse(shopId, roleFilter, pageable);
        } else {
            shopUsers = shopUserRepository.findByShopIdAndDeletedFalse(shopId, pageable);
        }

        List<String> userIds = shopUsers.map(ShopUser::getUserId).toList();
        Map<String, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<String, StaffProfile> profileMap = staffProfileRepository
                .findByShopIdAndUserIdInAndDeletedFalse(shopId, userIds).stream()
                .collect(Collectors.toMap(StaffProfile::getUserId, Function.identity()));

        List<ShopMemberResponse> filtered = shopUsers.getContent().stream()
                .map(su -> toMemberResponse(su, userMap.getOrDefault(su.getUserId(), new User()),
                        profileMap.get(su.getUserId())))
                .filter(member -> matchesKeyword(member, keyword))
                .toList();

        return new PageImpl<>(filtered, pageable, shopUsers.getTotalElements());
    }

    private boolean matchesKeyword(ShopMemberResponse member, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String kw = keyword.toLowerCase().trim();
        return (member.getFullName() != null && member.getFullName().toLowerCase().contains(kw))
                || (member.getEmail() != null && member.getEmail().toLowerCase().contains(kw))
                || (member.getPhone() != null && member.getPhone().toLowerCase().contains(kw));
    }

    private ShopMemberResponse toMemberResponse(ShopUser su, User user, StaffProfile profile) {
        ShopMemberResponse.ShopMemberResponseBuilder builder = ShopMemberResponse.builder()
                .id(su.getId())
                .userId(su.getUserId())
                .fullName(user.getFullName())
                .address(user.getAddress())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .city(user.getCity())
                .state(user.getState())
                .birthDate(user.getBirthDate())
                .createdAt(su.getCreatedAt())
                .gender(user.getGender())
                .role(su.getRole())
                .permissions(su.getPermissions());

        if (profile != null) {
            builder.branchId(profile.getBranchId());
            builder.position(profile.getPosition());
            builder.department(profile.getDepartment());
        }

        return builder.build();
    }

    public void markDeletedByShopId(String shopId) {
        List<ShopUser> shopUsers = shopUserRepository.findByShopId(shopId, Pageable.unpaged()).getContent();
        for (ShopUser shopUser : shopUsers) {
            shopUser.setDeleted(true);
        }
        shopUserRepository.saveAll(shopUsers);
    }
}
