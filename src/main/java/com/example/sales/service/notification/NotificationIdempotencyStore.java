package com.example.sales.service.notification;

import com.example.sales.model.NotificationDedupe;
import com.example.sales.repository.NotificationDedupeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Đảm bảo mỗi dedupeKey chỉ gửi 1 lần (trong TTL).
 *
 * Cơ chế: insert-if-absent trên unique index. Producer (scheduler) có thể
 * chạy lại nhiều lần mà không bị spam notification trùng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationIdempotencyStore {

    private final NotificationDedupeRepository repository;

    /**
     * @return true nếu đây là lần đầu thấy key (caller được phép gửi);
     *         false nếu đã gửi trước đó → caller nên bỏ qua.
     */
    public boolean tryAcquire(String dedupeKey) {
        if (dedupeKey == null || dedupeKey.isBlank()) return true;
        try {
            if (repository.existsByDedupeKey(dedupeKey)) {
                return false;
            }
            repository.save(NotificationDedupe.builder()
                    .dedupeKey(dedupeKey)
                    .createdAt(Instant.now())
                    .build());
            return true;
        } catch (DuplicateKeyException dup) {
            return false;
        } catch (Exception ex) {
            // Nếu store lỗi → fail-open: vẫn cho gửi, tránh chặn flow critical.
            log.warn("Idempotency store error for key {}: {}", dedupeKey, ex.getMessage());
            return true;
        }
    }
}
