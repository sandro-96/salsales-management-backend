// File: src/main/java/com/example/sales/validation/ValidDateRange.java
package com.example.sales.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DateRangeValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange {
    String message() default "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
