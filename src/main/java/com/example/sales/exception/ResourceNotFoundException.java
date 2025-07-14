// File: src/main/java/com/example/sales/exception/ResourceNotFoundException.java
package com.example.sales.exception;

import com.example.sales.constant.ApiCode;
import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final ApiCode error;

    public ResourceNotFoundException(ApiCode error) {
        super(error.name());
        this.error = error;
    }
}
