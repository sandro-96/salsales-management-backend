// File: src/main/java/com/example/sales/security/PermissionUtils.java
package com.example.sales.security;

import com.example.sales.constant.Permission;
import com.example.sales.constant.ShopRole;

import java.util.Set;

import static com.example.sales.constant.Permission.*;

/**
 * Quyền mặc định khi gán / đổi {@link ShopRole}.
 * Bản ghi {@code shop_users} đã tồn tại giữ nguyên {@code permissions} cho đến khi đổi vai trò hoặc cập nhật quyền thủ công.
 */
public class PermissionUtils {

    private static final Set<Permission> COMMON_VIEW = Set.of(
            ORDER_VIEW, PROMOTION_VIEW, TABLE_VIEW, BRANCH_VIEW, INVENTORY_VIEW, SHOP_VIEW, PRODUCT_VIEW
    );

    private static final Set<Permission> ORDER_PERMS = Set.of(
            ORDER_CREATE, ORDER_UPDATE, ORDER_PAYMENT_CONFIRM, ORDER_CANCEL
    );

    private static final Set<Permission> PRODUCT_BASE = Set.of(
            PRODUCT_VIEW,
            PRODUCT_CREATE, PRODUCT_UPDATE, PRODUCT_DELETE, PRODUCT_IMPORT,
            PRODUCT_EXPORT, PRODUCT_UPDATE_STATUS, PRODUCT_VIEW_LOW_STOCK
    );

    private static final Set<Permission> CUSTOMER_FULL = Set.of(
            CUSTOMER_VIEW, CUSTOMER_UPDATE, CUSTOMER_DELETE
    );

    private static final Set<Permission> PROMOTION_FULL = Set.of(
            PROMOTION_CREATE, PROMOTION_UPDATE, PROMOTION_DELETE, PROMOTION_VIEW
    );

    private static final Set<Permission> TABLE_FULL = Set.of(
            TABLE_CREATE, TABLE_UPDATE, TABLE_DELETE, TABLE_VIEW
    );

    private static final Set<Permission> SHOP_FULL = Set.of(
            SHOP_UPDATE, SHOP_DELETE, SHOP_VIEW
    );

    private static final Set<Permission> SHOP_USER_FULL = Set.of(
            SHOP_USER_CREATE, SHOP_USER_UPDATE, SHOP_USER_DELETE, SHOP_USER_VIEW
    );

    /** Thu ngân: POS + thanh toán; không xem kho; không CRUD sản phẩm/KM/bàn. */
    private static final Set<Permission> CASHIER_DEFAULT = merge(
            Set.of(ORDER_VIEW, ORDER_CREATE, ORDER_UPDATE, ORDER_PAYMENT_CONFIRM, ORDER_CANCEL),
            Set.of(PRODUCT_VIEW, PROMOTION_VIEW, TABLE_VIEW, BRANCH_VIEW, SHOP_VIEW,
                    CUSTOMER_VIEW, CUSTOMER_UPDATE)
    );

    public static Set<Permission> getDefaultPermissions(ShopRole role) {
        return switch (role) {
            case OWNER -> merge(
                    PRODUCT_BASE, ORDER_PERMS, CUSTOMER_FULL, SHOP_FULL, COMMON_VIEW,
                    Set.of(BRANCH_UPDATE, SHOP_MANAGE, REPORT_VIEW, BRANCH_MANAGE, INVENTORY_MANAGE, INVENTORY_VIEW),
                    PROMOTION_FULL, TABLE_FULL, SHOP_USER_FULL
            );
            case MANAGER -> merge(
                    PRODUCT_BASE, ORDER_PERMS, CUSTOMER_FULL, COMMON_VIEW,
                    Set.of(SHOP_USER_CREATE, SHOP_USER_UPDATE, SHOP_USER_VIEW, BRANCH_VIEW, REPORT_VIEW, INVENTORY_MANAGE, INVENTORY_VIEW, SHOP_UPDATE, SHOP_VIEW, BRANCH_MANAGE),
                    PROMOTION_FULL, TABLE_FULL
            );
            case STAFF -> merge(
                    Set.of(ORDER_CREATE, ORDER_UPDATE, CUSTOMER_VIEW), COMMON_VIEW
            );
            case CASHIER -> CASHIER_DEFAULT;
        };
    }

    @SafeVarargs
    private static Set<Permission> merge(Set<Permission>... sets) {
        return Set.of(sets).stream().flatMap(Set::stream).collect(java.util.stream.Collectors.toSet());
    }
}

