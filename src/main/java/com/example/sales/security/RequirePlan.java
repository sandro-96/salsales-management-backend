// File: com/example/sales/security/RequirePlan.java
package com.example.sales.security;

import com.example.sales.constant.SubscriptionPlan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePlan {
    SubscriptionPlan[] value() default {};
}

