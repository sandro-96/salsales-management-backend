// File: src/main/java/com/example/sales/security/RequireBranchRole.java
package com.example.sales.security;

import com.example.sales.constant.ShopRole;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireBranchRole {
    ShopRole[] value();
}