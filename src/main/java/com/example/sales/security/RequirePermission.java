// File: src/main/java/com/example/sales/security/RequirePermission.java
package com.example.sales.security;

import com.example.sales.constant.Permission;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    Permission value();
}
