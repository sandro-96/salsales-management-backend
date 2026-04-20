// File: src/main/java/com/example/sales/security/Audited.java
package com.example.sales.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Đánh dấu một endpoint admin cần ghi audit log tự động.
 * Aspect sẽ lấy resource/action/targetId (SpEL trên tham số) để build record.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {

    /** Ví dụ: SHOP / USER / BILLING / BROADCAST / CATALOG / AUTH. */
    String resource();

    /** Ví dụ: UPDATE_STATUS / UPDATE_PLAN / RESET_PASSWORD / SEND / DELETE. */
    String action();

    /** SpEL lấy targetId từ arg, ví dụ {@code #shopId}, {@code #req.id}. Có thể để rỗng. */
    String targetIdExpr() default "";

    /** SpEL cho nhãn ngắn gọn (label). Có thể để rỗng. */
    String targetLabelExpr() default "";
}
