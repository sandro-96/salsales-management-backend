package com.example.sales.service.notification;

import com.example.sales.constant.NotificationChannel;
import com.example.sales.dto.notification.NotificationEnvelope;
import com.example.sales.model.User;
import com.example.sales.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Điểm vào duy nhất cho mọi notification trong hệ thống.
 *
 * Producer chỉ cần build {@link NotificationEnvelope} và gọi {@link #dispatch}.
 * Dispatcher chịu trách nhiệm:
 *   1. Kiểm tra idempotency (nếu envelope có dedupeKey).
 *   2. Tra {@link NotificationRouter} để biết các channel cần kích hoạt.
 *   3. Áp forcedChannels của producer (override config mặc định).
 *   4. Resolve User cho mỗi recipientId (tái dùng cache theo 1 request).
 *   5. Gọi {@link NotificationSender} tương ứng, log lỗi từng cái
 *      mà không làm fail toàn bộ dispatch.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationRouter router;
    private final NotificationIdempotencyStore idempotencyStore;
    private final UserRepository userRepository;
    private final List<NotificationSender> senders;

    private Map<NotificationChannel, NotificationSender> senderMap;

    public void dispatch(NotificationEnvelope envelope) {
        if (envelope == null) return;
        if (envelope.getType() == null) {
            log.warn("Dispatch called with null type — skip");
            return;
        }
        List<String> recipients = envelope.getRecipientUserIds();
        if (recipients == null || recipients.isEmpty()) {
            log.debug("No recipients for {} — skip", envelope.getType());
            return;
        }

        if (!idempotencyStore.tryAcquire(envelope.getDedupeKey())) {
            log.debug("Skip duplicate notification dedupeKey={}", envelope.getDedupeKey());
            return;
        }

        Set<NotificationChannel> channels = resolveChannels(envelope);
        if (channels.isEmpty()) {
            log.debug("No channels configured for {} — skip", envelope.getType());
            return;
        }

        Map<String, User> userCache = loadUsers(recipients);

        for (String userId : recipients) {
            User user = userCache.get(userId);
            if (user == null) {
                log.debug("Skip recipient {} (not found/deleted) for {}", userId, envelope.getType());
                continue;
            }

            for (NotificationChannel channel : channels) {
                NotificationRouter.ChannelPlan plan = router.plan(envelope.getType(), channel);
                // forcedChannels có thể bật channel chưa config — dùng plan rỗng fallback.
                if (plan == null) plan = NotificationRouter.ChannelPlan.inApp();

                NotificationSender sender = senderMap().get(channel);
                if (sender == null) {
                    log.debug("No sender registered for channel {} — skip", channel);
                    continue;
                }
                try {
                    sender.send(envelope, user, plan);
                } catch (Exception ex) {
                    log.warn("Sender {} failed for user {} ({}): {}",
                            channel, userId, envelope.getType(), ex.getMessage());
                }
            }
        }
    }

    private Set<NotificationChannel> resolveChannels(NotificationEnvelope envelope) {
        Set<NotificationChannel> fromRouter = router.channelsFor(envelope.getType());
        Set<NotificationChannel> forced = envelope.getForcedChannels();
        if (forced == null || forced.isEmpty()) return fromRouter;

        Set<NotificationChannel> merged = EnumSet.copyOf(fromRouter);
        merged.addAll(forced);
        return merged;
    }

    private Map<String, User> loadUsers(List<String> ids) {
        Map<String, User> map = new HashMap<>();
        userRepository.findAllById(ids).forEach(u -> {
            if (!u.isDeleted()) map.put(u.getId(), u);
        });
        return map;
    }

    private Map<NotificationChannel, NotificationSender> senderMap() {
        if (senderMap == null) {
            Map<NotificationChannel, NotificationSender> m = new EnumMap<>(NotificationChannel.class);
            for (NotificationSender s : senders) {
                m.put(s.channel(), s);
            }
            senderMap = m;
        }
        return senderMap;
    }
}
