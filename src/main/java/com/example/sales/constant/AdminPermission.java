// File: src/main/java/com/example/sales/constant/AdminPermission.java
package com.example.sales.constant;

import java.util.EnumSet;
import java.util.Set;

/**
 * Quyền chi tiết áp dụng cho user có {@link UserRole#ROLE_ADMIN}.
 * <p>
 * Thiết kế song song với {@link Permission} (nội shop) nhưng áp dụng ở phạm vi
 * hệ thống. Mỗi user ROLE_ADMIN mang một {@code Set<AdminPermission>}; một số
 * preset tiện dụng được định nghĩa dưới dạng static set.
 */
public enum AdminPermission {
    DASHBOARD_VIEW,

    SHOP_VIEW,
    SHOP_MANAGE,

    USER_VIEW,
    USER_MANAGE,

    BILLING_VIEW,
    BILLING_MANAGE,

    CATALOG_MANAGE,

    SUPPORT_VIEW,
    SUPPORT_MANAGE,

    BROADCAST_SEND,

    AUDIT_VIEW,

    IMPERSONATE,

    SYSTEM_SETTINGS;

    /** Bundle preset dùng nhanh khi gán quyền trên UI. */
    public enum Preset {
        SUPER_ADMIN,
        SUPPORT_ADMIN,
        BILLING_ADMIN,
        CONTENT_ADMIN
    }

    public static Set<AdminPermission> presetPermissions(Preset preset) {
        return switch (preset) {
            case SUPER_ADMIN -> EnumSet.allOf(AdminPermission.class);
            case SUPPORT_ADMIN -> EnumSet.of(
                    DASHBOARD_VIEW,
                    SUPPORT_VIEW, SUPPORT_MANAGE,
                    SHOP_VIEW, USER_VIEW
            );
            case BILLING_ADMIN -> EnumSet.of(
                    DASHBOARD_VIEW,
                    BILLING_VIEW, BILLING_MANAGE,
                    SHOP_VIEW, USER_VIEW
            );
            case CONTENT_ADMIN -> EnumSet.of(
                    DASHBOARD_VIEW,
                    CATALOG_MANAGE,
                    BROADCAST_SEND
            );
        };
    }
}
