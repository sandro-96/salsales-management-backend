package com.example.sales.config;

import com.example.sales.constant.Permission;
import com.example.sales.constant.ShopRole;
import com.example.sales.security.PermissionUtils;
import lombok.RequiredArgsConstructor;
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

/**
 * Gỡ {@code ShopRole.ADMIN}: cập nhật bản ghi cũ trong {@code shop_users} thành MANAGER
 * và quyền mặc định của MANAGER. Quản trị hệ thống chỉ còn {@code UserRole.ROLE_ADMIN}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ShopRoleAdminMongoMigration implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<String> permNames = new ArrayList<>();
        for (Permission p : PermissionUtils.getDefaultPermissions(ShopRole.MANAGER)) {
            permNames.add(p.name());
        }

        Query query = Query.query(Criteria.where("role").is("ADMIN"));
        Update update = new Update()
                .set("role", ShopRole.MANAGER.name())
                .set("permissions", permNames);
        mongoTemplate.updateMulti(query, update, "shop_users");
    }
}
