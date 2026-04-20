// File: src/main/java/com/example/sales/service/admin/AdminBroadcastService.java
package com.example.sales.service.admin;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.BroadcastAudience;
import com.example.sales.constant.BroadcastStatus;
import com.example.sales.constant.NotificationChannel;
import com.example.sales.constant.NotificationType;
import com.example.sales.constant.SubscriptionPlan;
import com.example.sales.constant.UserRole;
import com.example.sales.dto.admin.AdminBroadcastRequest;
import com.example.sales.dto.admin.AdminBroadcastResponse;
import com.example.sales.dto.notification.NotificationEnvelope;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Broadcast;
import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.repository.BroadcastRepository;
import com.example.sales.service.notification.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Xử lý broadcast do admin khởi tạo: xác định danh sách recipient theo audience,
 * đẩy sang {@link NotificationDispatcher} để các sender (in-app/email) chạy
 * theo router hiện có. Không tự mở transaction — các bước phụ (lưu, set status)
 * được log riêng để tránh rollback vì một lỗi dispatch cá nhân.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBroadcastService {

    private final BroadcastRepository broadcastRepository;
    private final MongoTemplate mongoTemplate;
    private final NotificationDispatcher dispatcher;

    public Page<AdminBroadcastResponse> list(Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted()
                ? pageable
                : org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return broadcastRepository.findAllByDeletedFalse(sorted).map(this::toResponse);
    }

    public AdminBroadcastResponse send(AdminBroadcastRequest req, String adminId) {
        if (req.getAudience() == BroadcastAudience.SHOPS_BY_PLAN && req.getPlan() == null) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        Broadcast entity = Broadcast.builder()
                .title(req.getTitle())
                .message(req.getMessage())
                .audience(req.getAudience())
                .plan(req.getPlan())
                .emailEnabled(req.isEmailEnabled())
                .status(BroadcastStatus.SENDING)
                .build();
        entity.setCreatedBy(adminId);
        entity = broadcastRepository.save(entity);

        Set<String> recipients;
        try {
            recipients = resolveRecipients(req);
        } catch (RuntimeException ex) {
            entity.setStatus(BroadcastStatus.FAILED);
            broadcastRepository.save(entity);
            throw ex;
        }

        int sent = 0;
        int failed = 0;
        for (String userId : recipients) {
            try {
                NotificationEnvelope.NotificationEnvelopeBuilder builder = NotificationEnvelope.builder()
                        .type(NotificationType.BROADCAST)
                        .recipient(userId)
                        .title(req.getTitle())
                        .message(req.getMessage())
                        .referenceId(entity.getId())
                        .referenceType("BROADCAST")
                        .actorId(adminId)
                        .dedupeKey("broadcast:" + entity.getId() + ":" + userId);
                if (req.isEmailEnabled()) {
                    builder.forceChannel(NotificationChannel.IN_APP)
                            .forceChannel(NotificationChannel.EMAIL);
                }
                dispatcher.dispatch(builder.build());
                sent++;
            } catch (Exception ex) {
                failed++;
                log.warn("Broadcast {} fail userId={}: {}", entity.getId(), userId, ex.getMessage());
            }
        }

        entity.setRecipientCount(sent);
        entity.setSentAt(LocalDateTime.now());
        entity.setStatus(failed > 0 && sent == 0 ? BroadcastStatus.FAILED : BroadcastStatus.SENT);
        entity = broadcastRepository.save(entity);
        log.info("[AdminBroadcast] id={} audience={} sent={} failed={} by adminId={}",
                entity.getId(), req.getAudience(), sent, failed, adminId);
        return toResponse(entity);
    }

    // ───────────────────────────────────────────────────────────────────────

    private Set<String> resolveRecipients(AdminBroadcastRequest req) {
        return switch (req.getAudience()) {
            case ALL_USERS -> activeUserIds(Criteria.where("deleted").is(false).and("active").is(true));
            case ADMINS -> activeUserIds(Criteria.where("deleted").is(false)
                    .and("active").is(true)
                    .and("role").is(UserRole.ROLE_ADMIN.name()));
            case SHOP_OWNERS -> ownerIdsForShops(Criteria.where("deleted").is(false)
                    .and("active").is(true));
            case SHOPS_BY_PLAN -> ownerIdsForShops(Criteria.where("deleted").is(false)
                    .and("active").is(true)
                    .and("plan").is(req.getPlan().name()));
        };
    }

    private Set<String> activeUserIds(Criteria c) {
        List<User> users = mongoTemplate.find(Query.query(c), User.class);
        return users.stream().map(User::getId).collect(Collectors.toCollection(HashSet::new));
    }

    private Set<String> ownerIdsForShops(Criteria shopCriteria) {
        List<Shop> shops = mongoTemplate.find(Query.query(shopCriteria), Shop.class);
        return shops.stream()
                .map(Shop::getOwnerId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private AdminBroadcastResponse toResponse(Broadcast b) {
        return AdminBroadcastResponse.builder()
                .id(b.getId())
                .title(b.getTitle())
                .message(b.getMessage())
                .audience(b.getAudience())
                .plan(b.getPlan())
                .emailEnabled(b.isEmailEnabled())
                .status(b.getStatus())
                .recipientCount(b.getRecipientCount())
                .sentAt(b.getSentAt())
                .createdAt(b.getCreatedAt())
                .createdBy(b.getCreatedBy())
                .build();
    }

    @SuppressWarnings("unused")
    private SubscriptionPlan planIfNeeded(SubscriptionPlan plan) {
        return plan;
    }
}
