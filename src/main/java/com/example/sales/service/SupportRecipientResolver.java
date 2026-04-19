package com.example.sales.service;

import com.example.sales.constant.UserRole;
import com.example.sales.model.User;
import com.example.sales.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Resolve danh sách user system-admin (ROLE_ADMIN) cho các flow fan-out thông báo.
 *
 * <p>Admin users thay đổi rất ít; cache 60s trong bộ nhớ để tránh query MongoDB
 * mỗi lần có ticket / reply.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SupportRecipientResolver {

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private final UserRepository userRepository;

    private final AtomicReference<CachedAdmins> cache = new AtomicReference<>(null);

    /** Lấy ID của toàn bộ system admin. */
    public List<String> getAdminUserIds() {
        return load().ids;
    }

    /** Lấy full entity (cần email để gửi mail HIGH/URGENT). */
    public List<User> getAdminUsers() {
        return load().users;
    }

    /** Cho phép service chủ động invalidate khi có thay đổi role. */
    public void invalidate() {
        cache.set(null);
    }

    private CachedAdmins load() {
        CachedAdmins current = cache.get();
        Instant now = Instant.now();
        if (current != null && current.expiresAt.isAfter(now)) {
            return current;
        }
        try {
            List<User> users = userRepository.findByRoleAndDeletedFalse(UserRole.ROLE_ADMIN);
            List<String> ids = users.stream().map(User::getId).toList();
            CachedAdmins fresh = new CachedAdmins(users, ids, now.plus(CACHE_TTL));
            cache.set(fresh);
            return fresh;
        } catch (Exception ex) {
            log.warn("Failed to load admin recipients: {}", ex.getMessage());
            return new CachedAdmins(Collections.emptyList(), Collections.emptyList(), now.plus(Duration.ofSeconds(5)));
        }
    }

    private record CachedAdmins(List<User> users, List<String> ids, Instant expiresAt) {
    }
}
