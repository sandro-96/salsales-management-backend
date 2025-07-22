// File: src/main/java/com/example/sales/security/PermissionUtils.java
package com.example.sales.security;

import com.example.sales.constant.Permission;
import com.example.sales.constant.ShopRole;

import java.util.Set;

import static com.example.sales.constant.Permission.*;

public class PermissionUtils {

    public static Set<Permission> getDefaultPermissions(ShopRole role) {
        return switch (role) {
            case OWNER -> Set.of(
                    PRODUCT_CREATE, PRODUCT_UPDATE, PRODUCT_DELETE,
                    ORDER_CREATE, ORDER_UPDATE, ORDER_CANCEL,
                    CUSTOMER_VIEW, CUSTOMER_UPDATE, CUSTOMER_DELETE,
                    BRANCH_UPDATE, SHOP_MANAGE, PRODUCT_IMPORT,
                    PRODUCT_EXPORT, PRODUCT_UPDATE_STATUS,
                    PRODUCT_VIEW_LOW_STOCK,
                    ORDER_PAYMENT_CONFIRM,
                    ORDER_VIEW,
                    PROMOTION_CREATE,
                    PROMOTION_UPDATE,
                    PROMOTION_DELETE,
                    PROMOTION_VIEW,
                    TABLE_CREATE, TABLE_UPDATE, TABLE_DELETE, TABLE_VIEW,
                    SHOP_USER_CREATE, SHOP_USER_UPDATE, SHOP_USER_DELETE, SHOP_USER_VIEW, SHOP_USER_BRANCH_DELETE,
                    BRANCH_MANAGE, INVENTORY_MANAGE, INVENTORY_VIEW,
                    REPORT_VIEW
            );
            case MANAGER -> Set.of(
                    PRODUCT_CREATE, PRODUCT_UPDATE,
                    ORDER_CREATE, ORDER_UPDATE,
                    CUSTOMER_VIEW, CUSTOMER_UPDATE, CUSTOMER_DELETE, PRODUCT_IMPORT,
                    PRODUCT_EXPORT, PRODUCT_UPDATE_STATUS,
                    PRODUCT_VIEW_LOW_STOCK,
                    ORDER_PAYMENT_CONFIRM,
                    ORDER_VIEW,
                    PROMOTION_CREATE,
                    PROMOTION_UPDATE,
                    PROMOTION_DELETE,
                    PROMOTION_VIEW,
                    TABLE_CREATE, TABLE_UPDATE, TABLE_DELETE, TABLE_VIEW,
                    SHOP_USER_CREATE, SHOP_USER_UPDATE, SHOP_USER_VIEW, SHOP_USER_BRANCH_DELETE,
                    BRANCH_VIEW, INVENTORY_MANAGE, INVENTORY_VIEW,
                    REPORT_VIEW
            );
            case CASHIER -> Set.of(
                    ORDER_CREATE, ORDER_UPDATE,
                    CUSTOMER_VIEW, PRODUCT_EXPORT,
                    ORDER_PAYMENT_CONFIRM,
                    ORDER_VIEW,
                    PROMOTION_VIEW,
                    TABLE_VIEW,
                    BRANCH_VIEW, INVENTORY_MANAGE, INVENTORY_VIEW
            );
            case STAFF -> Set.of(
                    ORDER_CREATE, ORDER_UPDATE,
                    CUSTOMER_VIEW,
                    ORDER_VIEW,
                    PROMOTION_VIEW,
                    TABLE_VIEW,
                    BRANCH_VIEW, INVENTORY_VIEW
            );
            default -> Set.of();
        };
    }
}
