// File: src/main/java/com/example/sales/service/admin/AdminUserService.java
package com.example.sales.service.admin;

import com.example.sales.constant.AdminPermission;
import com.example.sales.constant.ApiCode;
import com.example.sales.constant.UserRole;
import com.example.sales.dto.admin.AdminUserDetail;
import com.example.sales.dto.admin.AdminUserPermissionsRequest;
import com.example.sales.dto.admin.AdminUserStatusUpdateRequest;
import com.example.sales.dto.admin.AdminUserSummary;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Shop;
import com.example.sales.model.ShopUser;
import com.example.sales.model.User;
import com.example.sales.repository.UserRepository;
import com.example.sales.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Quản trị user toàn hệ thống (read-only trong Phase 1).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;
    private final MailService mailService;

    @Value("${app.fe.url:}")
    private String feUrl;

    @Value("${app.reset-token.expiry-minutes:15}")
    private long resetTokenExpiryMinutes;

    public Page<AdminUserSummary> list(Pageable pageable, UserRole role, String status, String keyword) {
        Criteria criteria = Criteria.where("deleted").is(false);
        if (role != null) criteria = criteria.and("role").is(role.name());
        if ("active".equalsIgnoreCase(status)) criteria = criteria.and("active").is(true);
        else if ("locked".equalsIgnoreCase(status)) criteria = criteria.and("active").is(false);

        if (StringUtils.hasText(keyword)) {
            String regex = ".*" + java.util.regex.Pattern.quote(keyword.trim()) + ".*";
            criteria = criteria.orOperator(
                    Criteria.where("email").regex(regex, "i"),
                    Criteria.where("firstName").regex(regex, "i"),
                    Criteria.where("lastName").regex(regex, "i"),
                    Criteria.where("phone").regex(regex, "i")
            );
        }

        Query query = Query.query(criteria).with(pageable);
        long total = mongoTemplate.count(Query.query(criteria), User.class);
        List<User> users = mongoTemplate.find(query, User.class);

        Set<String> userIds = users.stream().map(User::getId).collect(Collectors.toSet());
        Map<String, Long> ownedShopCounts = countOwnedShops(userIds);
        Map<String, Long> memberShopCounts = countMemberShops(userIds);

        List<AdminUserSummary> summaries = users.stream()
                .map(u -> toSummary(u,
                        ownedShopCounts.getOrDefault(u.getId(), 0L),
                        memberShopCounts.getOrDefault(u.getId(), 0L)))
                .collect(Collectors.toList());
        return new PageImpl<>(summaries, pageable, total);
    }

    public AdminUserDetail detail(String userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND));

        long owned = countOwnedShops(Set.of(userId)).getOrDefault(userId, 0L);
        long member = countMemberShops(Set.of(userId)).getOrDefault(userId, 0L);
        AdminUserSummary summary = toSummary(user, owned, member);

        // Memberships = shops nơi user là owner HOẶC có trong shop_users
        List<AdminUserDetail.ShopMembership> memberships = new ArrayList<>();

        // Owned shops (owner)
        List<Shop> ownedShops = mongoTemplate.find(
                Query.query(Criteria.where("deleted").is(false).and("ownerId").is(userId)),
                Shop.class);
        for (Shop s : ownedShops) {
            memberships.add(AdminUserDetail.ShopMembership.builder()
                    .shopId(s.getId())
                    .shopName(s.getName())
                    .role(null)
                    .owner(true)
                    .build());
        }
        // Joined via shop_users
        List<ShopUser> joins = mongoTemplate.find(
                Query.query(Criteria.where("deleted").is(false).and("userId").is(userId)),
                ShopUser.class);
        if (!joins.isEmpty()) {
            Set<String> shopIds = joins.stream().map(ShopUser::getShopId).collect(Collectors.toSet());
            List<Shop> shops = mongoTemplate.find(
                    Query.query(Criteria.where("deleted").is(false)
                            .and("_id").in(shopIds)),
                    Shop.class);
            Map<String, Shop> shopMap = new HashMap<>();
            for (Shop s : shops) shopMap.put(s.getId(), s);
            for (ShopUser su : joins) {
                Shop s = shopMap.get(su.getShopId());
                memberships.add(AdminUserDetail.ShopMembership.builder()
                        .shopId(su.getShopId())
                        .shopName(s != null ? s.getName() : null)
                        .role(su.getRole())
                        .owner(false)
                        .build());
            }
        }

        return AdminUserDetail.builder().summary(summary).memberships(memberships).build();
    }

    // ─── Write operations (Phase 2) ────────────────────────────────────────

    public AdminUserSummary updateStatus(String userId, AdminUserStatusUpdateRequest req, String adminId) {
        if (userId.equals(adminId)) {
            // Không cho admin tự khoá chính mình.
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND));
        user.setActive(Boolean.TRUE.equals(req.getActive()));
        userRepository.save(user);
        log.info("[AdminUser] {} set active={} userId={} reason='{}'",
                adminId, user.isActive(), userId, req.getReason());
        long owned = countOwnedShops(Set.of(userId)).getOrDefault(userId, 0L);
        long member = countMemberShops(Set.of(userId)).getOrDefault(userId, 0L);
        return toSummary(user, owned, member);
    }

    public AdminUserSummary updateAdminPermissions(String userId, AdminUserPermissionsRequest req, String adminId) {
        if (userId.equals(adminId)) {
            // Admin không được tự gỡ quyền mình — tránh khoá cổ chai.
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND));
        if (user.getRole() != UserRole.ROLE_ADMIN) {
            // Chỉ đặt quyền admin cho user đã là admin — nâng cấp role là flow khác.
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }

        Set<AdminPermission> merged = EnumSet.noneOf(AdminPermission.class);
        if (req.getPreset() != null) {
            merged.addAll(AdminPermission.presetPermissions(req.getPreset()));
        }
        if (req.getPermissions() != null) {
            merged.addAll(req.getPermissions());
        }
        user.setAdminPermissions(merged);
        userRepository.save(user);
        log.info("[AdminUser] {} set adminPermissions={} userId={}", adminId, merged, userId);

        long owned = countOwnedShops(Set.of(userId)).getOrDefault(userId, 0L);
        long member = countMemberShops(Set.of(userId)).getOrDefault(userId, 0L);
        return toSummary(user, owned, member);
    }

    public void resetPassword(String userId, String adminId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND));
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(Instant.now().plusSeconds(resetTokenExpiryMinutes * 60));
        userRepository.save(user);

        String resetLink = (feUrl == null ? "" : feUrl) + "/reset-password?token=" + token;
        String html = "<p>Xin chào " + (user.getFullName() != null ? user.getFullName() : "") + ",</p>" +
                "<p>Quản trị viên đã khởi tạo yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>" +
                "<p>Vui lòng nhấn vào liên kết sau để tiếp tục:</p>" +
                "<a href=\"" + resetLink + "\">Đặt lại mật khẩu</a>" +
                "<p><i>Liên kết này sẽ hết hạn sau " + resetTokenExpiryMinutes + " phút.</i></p>";
        try {
            mailService.send(user.getEmail(), "[Admin] Đặt lại mật khẩu - Sandro Sales", html);
        } catch (Exception ex) {
            log.warn("Không thể gửi mail reset password tới {}: {}", user.getEmail(), ex.getMessage());
        }
        log.info("[AdminUser] {} triggered password reset userId={}", adminId, userId);
    }

    public void resendVerification(String userId, String adminId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND));
        if (user.isVerified()) {
            throw new BusinessException(ApiCode.ALREADY_VERIFIED);
        }
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setVerificationExpiry(Instant.now().plusSeconds(resetTokenExpiryMinutes * 60));
        userRepository.save(user);

        String verifyLink = (feUrl == null ? "" : feUrl) + "/verify?token=" + token;
        String html = "<p>Xin chào,</p>" +
                "<p>Quản trị viên đã gửi lại email xác thực tài khoản của bạn.</p>" +
                "<a href=\"" + verifyLink + "\">Xác thực tài khoản</a>" +
                "<p><i>Liên kết này sẽ hết hạn sau " + resetTokenExpiryMinutes + " phút.</i></p>";
        try {
            mailService.send(user.getEmail(), "[Admin] Xác thực tài khoản - Sandro Sales", html);
        } catch (Exception ex) {
            log.warn("Không thể gửi mail verify tới {}: {}", user.getEmail(), ex.getMessage());
        }
        log.info("[AdminUser] {} triggered resend verification userId={}", adminId, userId);
    }

    private Map<String, Long> countOwnedShops(Set<String> userIds) {
        if (userIds.isEmpty()) return Map.of();
        List<Shop> shops = mongoTemplate.find(
                Query.query(Criteria.where("deleted").is(false).and("ownerId").in(userIds)),
                Shop.class);
        Map<String, Long> map = new HashMap<>();
        for (Shop s : shops) {
            map.merge(s.getOwnerId(), 1L, Long::sum);
        }
        return map;
    }

    private Map<String, Long> countMemberShops(Set<String> userIds) {
        if (userIds.isEmpty()) return Map.of();
        List<ShopUser> joins = mongoTemplate.find(
                Query.query(Criteria.where("deleted").is(false).and("userId").in(userIds)),
                ShopUser.class);
        Map<String, Set<String>> perUser = new HashMap<>();
        for (ShopUser su : joins) {
            perUser.computeIfAbsent(su.getUserId(), k -> new HashSet<>()).add(su.getShopId());
        }
        Map<String, Long> map = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : perUser.entrySet()) {
            map.put(e.getKey(), (long) e.getValue().size());
        }
        return map;
    }

    private AdminUserSummary toSummary(User user, long ownedCount, long memberCount) {
        return AdminUserSummary.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .active(user.isActive())
                .verified(user.isVerified())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .adminPermissions(user.getAdminPermissions())
                .ownedShopCount(ownedCount)
                .memberShopCount(memberCount)
                .build();
    }

}
