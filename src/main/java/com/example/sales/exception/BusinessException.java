// File: src/main/java/com/example/sales/exception/BusinessException.java
package com.example.sales.exception;

import com.example.sales.constant.ApiCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ApiCode error;

    public BusinessException(ApiCode error) {
        super(error.name());
        this.error = error;
    }
}