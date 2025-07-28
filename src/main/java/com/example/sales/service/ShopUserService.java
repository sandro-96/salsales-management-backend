// File: src/main/java/com/example/sales/service/ShopUserService.java
package com.example.sales.service;

import com.example.sales.cache.ShopUserCache;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.shop.ShopSimpleResponse;
import com.example.sales.dto.shopUser.ShopStaffResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Branch;
import com.example.sales.model.Shop;
import com.example.sales.model.ShopUser;
import com.example.sales.model.User;
import com.example.sales.repository.BranchRepository;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.ShopUserRepository;
import com.example.sales.repository.UserRepository;
import com.example.sales.security.PermissionUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopUserService extends BaseService {

    private final ShopUserRepository shopUserRepository;
    private final ShopRepository shopRepository;
    private final AuditLogService auditLogService;
    private final BranchRepository branchRepository;
    private final ShopUserCache shopUserCache;
    private final UserRepository userRepository;

    public void requireAnyRole(String shopId, String userId, ShopRole... roles) {
        ShopRole actual = shopUserCache.getUserRoleInShop(shopId, userId);
        if (Arrays.stream(roles).noneMatch(role -> role == actual)) {
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }
    }

    public void addUser(String shopId, String userId, ShopRole role, String branchId, String performedByUserId) {
        validateBranchRoleHierarchy(shopId, branchId, performedByUserId, userId);
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        if (!shop.isActive()) {
            throw new BusinessException(ApiCode.SHOP_INACTIVE);
        }

        Optional<ShopUser> existingShopUser = shopUserRepository.findByShopIdAndUserIdAndBranchId(shopId, userId, branchId);

        if (existingShopUser.isPresent()) {
            ShopUser shopUser = existingShopUser.get();
            if (!shopUser.isDeleted()) {
                throw new BusinessException(ApiCode.USER_ALREADY_IN_SHOP);
            } else {
                shopUser.setDeleted(false);
                shopUser.setRole(role);
                shopUser.setPermissions(PermissionUtils.getDefaultPermissions(role));
                shopUserRepository.save(shopUser);
                auditLogService.log(performedByUserId, shopId, shopUser.getId(), "SHOP_USER", "REACTIVATED",
                        String.format("Tái kích hoạt người dùng %s vào chi nhánh %s của cửa hàng %s với vai trò %s", userId, branchId, shopId, role));
            }
        } else {
            ShopUser newShopUser = ShopUser.builder()
                    .shopId(shopId)
                    .userId(userId)
                    .role(role)
                    .branchId(branchId)
                    .permissions(PermissionUtils.getDefaultPermissions(role))
                    .build();
            shopUserRepository.save(newShopUser);
            auditLogService.log(performedByUserId, shopId, newShopUser.getId(), "SHOP_USER", "ADDED",
                    String.format("Thêm người dùng %s vào chi nhánh %s của cửa hàng %s với vai trò %s", userId, branchId, shopId, role));
        }
    }

    public void removeUser(String shopId, String userId, String branchId, String performedByUserId) {
        validateBranchRoleHierarchy(shopId, branchId, performedByUserId, userId);
        ShopUser shopUser = shopUserRepository.findByUserIdAndShopIdAndBranchIdAndDeletedFalse(userId, shopId, branchId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND));
        shopUser.setDeleted(true);
        shopUserRepository.save(shopUser);

        auditLogService.log(performedByUserId, shopId, shopUser.getId(), "SHOP_USER", "REMOVED",
                String.format("Xoá người dùng %s khỏi chi nhánh %s của cửa hàng %s", userId, branchId, shopId));
    }

    // ✅ Phương thức mới để xóa user khỏi shop, không quan tâm branch
    public void removeUserFromShop(String shopId, String userId, String performedByUserId) {
        if (userId.equals(performedByUserId)) {
            auditLogService.log(performedByUserId, shopId, userId, "SHOP_USER", "UNAUTHORIZED",
                    "Người dùng không thể xóa chính mình khỏi cửa hàng");
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }

        List<ShopUser> targetShopUsers = shopUserRepository.findByUserIdAndShopIdAndDeletedFalse(userId, shopId);
        if (targetShopUsers.isEmpty()) {
            auditLogService.log(performedByUserId, shopId, userId, "SHOP_USER", "NOT_FOUND",
                    String.format("Người dùng %s không tồn tại trong cửa hàng %s", userId, shopId));
            throw new BusinessException(ApiCode.NOT_FOUND);
        }

        // Lấy các bản ghi ShopUser của người thực hiện trong shop
        List<ShopUser> performerShopUsers = shopUserRepository.findByUserIdAndShopIdAndDeletedFalse(performedByUserId, shopId);
        if (performerShopUsers.isEmpty()) {
            auditLogService.log(performedByUserId, shopId, userId, "SHOP_USER", "ACCESS_DENIED",
                    String.format("Người dùng %s không có quyền thao tác trong cửa hàng %s", performedByUserId, shopId));
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }

        for (ShopUser targetSU : targetShopUsers) {
            ShopUser performer = performerShopUsers.stream()
                    .filter(p -> p.getBranchId().equals(targetSU.getBranchId()))
                    .findFirst()
                    .orElse(null);

            if (performer == null) {
                auditLogService.log(performedByUserId, shopId, userId, "SHOP_USER", "ACCESS_DENIED",
                        String.format("Người dùng %s không có quyền thao tác trong chi nhánh %s của cửa hàng %s", performedByUserId, targetSU.getBranchId(), shopId));
                throw new BusinessException(ApiCode.ACCESS_DENIED);
            }

            ensureCanModifyRole(performer.getRole(), targetSU.getRole());
            targetSU.setDeleted(true);
        }

        shopUserRepository.saveAll(targetShopUsers);

        auditLogService.log(performedByUserId, shopId, userId, "SHOP_USER", "REMOVED_FROM_SHOP",
                String.format("Xoá người dùng %s khỏi tất cả các chi nhánh của cửa hàng %s", userId, shopId));
    }


    public Page<ShopSimpleResponse> getShopsForUser(String userId, Pageable pageable) {
        return shopUserRepository.findByUserIdAndDeletedFalse(userId, pageable)
                .map(su -> {
                    Optional<Shop> shopOpt = shopRepository.findByIdAndDeletedFalse(su.getShopId());
                    Optional<Branch> branchOpt = branchRepository.findByIdAndDeletedFalse(su.getBranchId());

                    if (shopOpt.isEmpty() || branchOpt.isEmpty()) return null;

                    Shop shop = shopOpt.get();
                    Branch branch = branchOpt.get();

                    return ShopSimpleResponse.builder()
                            .id(shop.getId())
                            .name(shop.getName())
                            .type(shop.getType())
                            .logoUrl(shop.getLogoUrl())
                            .active(shop.isActive())
                            .role(su.getRole())
                            .branchId(branch.getId())
                            .branchName(branch.getName())
                            .branchAddress(branch.getAddress())
                            .industry(shop.getType().getIndustry())
                            .build();
                });
    }


    private void ensureCanModifyRole(ShopRole actorRole, ShopRole targetRole) {
        if (actorRole == ShopRole.MANAGER &&
                (targetRole == ShopRole.OWNER || targetRole == ShopRole.ADMIN || targetRole == ShopRole.MANAGER)) {
            auditLogService.log(null, null, null, "ROLE_MODIFICATION", "DENIED",
                    "MANAGER attempted to modify a higher role.");
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }

        if (actorRole.ordinal() < targetRole.ordinal()) {
            auditLogService.log(null, null, null, "ROLE_MODIFICATION", "DENIED",
                    String.format("User with role %s attempted to modify user with role %s.", actorRole, targetRole));
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
    }


    public ShopRole getUserRoleInBranch(String shopId, String userId, String branchId) {
        return shopUserRepository.findByUserIdAndShopIdAndBranchIdAndDeletedFalse(userId, shopId, branchId)
                .map(ShopUser::getRole)
                .orElseThrow(() -> new BusinessException(ApiCode.UNAUTHORIZED));
    }

    public void validateBranchRoleHierarchy(String shopId, String branchId, String actorUserId, String targetUserId) {
        // Lấy role người thực hiện (actor) tại branch
        ShopRole actorRole = getUserRoleInBranch(shopId, actorUserId, branchId);

        // Lấy role người bị thao tác (target)
        ShopRole targetRole = getUserRoleInBranch(shopId, targetUserId, branchId);

        // Thực hiện logic so sánh
        ensureCanModifyRole(actorRole, targetRole);
    }

    public Page<ShopStaffResponse> getUsersInShop(String shopId, String userId, String branchId, Pageable pageable) {
        // Kiểm tra cửa hàng có tồn tại và đang hoạt động
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
        if (!shop.isActive()) {
            throw new BusinessException(ApiCode.SHOP_INACTIVE);
        }

        // Nếu branchId được cung cấp, kiểm tra chi nhánh có tồn tại
        if (branchId != null && !branchId.isEmpty()) {
            branchRepository.findByIdAndDeletedFalse(branchId)
                    .orElseThrow(() -> new BusinessException(ApiCode.BRANCH_NOT_FOUND));
        }

        // Lấy danh sách người dùng trong cửa hàng, lọc theo branchId nếu có
        Page<ShopUser> shopUsers = branchId != null && !branchId.isEmpty()
                ? shopUserRepository.findByShopIdAndBranchIdAndDeletedFalse(shopId, branchId, pageable)
                : shopUserRepository.findByShopIdAndDeletedFalse(shopId, pageable);
        List<String> userIds = shopUsers.map(ShopUser::getUserId).toList();
        Map<String, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return shopUsers.map(su -> {
            Optional<Branch> branchOpt = branchRepository.findByIdAndDeletedFalse(su.getBranchId());
            String branchName = branchOpt.map(Branch::getName).orElse("Unknown");
            String branchAddress = branchOpt.map(Branch::getAddress).orElse("Unknown");

            return ShopStaffResponse.builder()
                    .userId(su.getUserId())
                    .name(userMap.getOrDefault(su.getUserId(), new User()).getFullName())
                    .email(userMap.getOrDefault(su.getUserId(), new User()).getEmail())
                    .phone(userMap.getOrDefault(su.getUserId(), new User()).getPhone())
                    .avatarUrl(userMap.getOrDefault(su.getUserId(), new User()).getAvatarUrl())
                    .shopId(su.getShopId())
                    .shopName(shop.getName())
                    .role(su.getRole())
                    .branchId(su.getBranchId())
                    .branchName(branchName)
                    .branchAddress(branchAddress)
                    .build();
        });
    }

}
