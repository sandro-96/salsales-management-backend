// File: src/main/java/com/example/sales/security/RequireRole.java
package com.example.sales.security;

import com.example.sales.constant.ShopRole;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    ShopRole[] value();
}
