// File: src/main/java/com/example/sales/config/AdminPermissionMigration.java
package com.example.sales.config;

import com.example.sales.constant.AdminPermission;
import com.example.sales.constant.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Seed {@code adminPermissions} cho mọi user có {@code role=ROLE_ADMIN}
 * đang thiếu field (tương thích ngược). Mặc định dùng preset SUPER_ADMIN
 * để duy trì hành vi "admin toàn năng" trước khi tính năng granular được
 * khai thác; super-admin có thể điều chỉnh qua API về sau.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class AdminPermissionMigration implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Set<AdminPermission> superAdminPerms =
                AdminPermission.presetPermissions(AdminPermission.Preset.SUPER_ADMIN);
        List<String> permNames = new ArrayList<>();
        for (AdminPermission p : superAdminPerms) {
            permNames.add(p.name());
        }

        // Chỉ seed cho admin chưa có field hoặc có field rỗng — tránh overwrite.
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("role").is(UserRole.ROLE_ADMIN.name()),
                new Criteria().orOperator(
                        Criteria.where("adminPermissions").exists(false),
                        Criteria.where("adminPermissions").size(0),
                        Criteria.where("adminPermissions").is(null)
                )
        );
        Query query = Query.query(criteria);
        Update update = new Update().set("adminPermissions", permNames);
        long updated = mongoTemplate.updateMulti(query, update, "users").getModifiedCount();
        if (updated > 0) {
            log.info("[AdminPermissionMigration] seed SUPER_ADMIN cho {} admin user", updated);
        }
    }
}
