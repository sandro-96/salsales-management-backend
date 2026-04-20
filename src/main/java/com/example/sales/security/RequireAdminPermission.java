// File: src/main/java/com/example/sales/security/RequireAdminPermission.java
package com.example.sales.security;

import com.example.sales.constant.AdminPermission;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Khai báo quyền admin yêu cầu cho controller method bên trong
 * {@code /api/admin/**}. Được kiểm tại {@link RequireAdminPermissionAspect}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAdminPermission {
    AdminPermission value();
}
