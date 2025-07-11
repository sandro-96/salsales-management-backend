// File: src/main/java/com/example/sales/exception/BusinessException.java
package com.example.sales.exception;

import com.example.sales.constant.ApiErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ApiErrorCode error;

    public BusinessException(ApiErrorCode error) {
        super(error.getMessage());
        this.error = error;
    }
}
