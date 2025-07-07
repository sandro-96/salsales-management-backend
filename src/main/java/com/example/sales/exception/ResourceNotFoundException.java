package com.example.sales.exception;

import com.example.sales.constant.ApiErrorCode;
import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final ApiErrorCode error;

    public ResourceNotFoundException(ApiErrorCode error) {
        super(error.getMessage());
        this.error = error;
    }
}