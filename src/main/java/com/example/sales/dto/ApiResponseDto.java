// File: src/main/java/com/example/sales/dto/ApiResponseDto.java
package com.example.sales.dto;

import com.example.sales.constant.ApiCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Generic response class for API responses, including success status, error code, message, data, and timestamp.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDto<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
    private String timestamp;

    /**
     * Creates a successful response with data.
     */
    public static <T> ApiResponseDto<T> success(ApiCode code, T data) {
        return new ApiResponseDto<>(true, code.getCode(), code.getMessage(), data, Instant.now().toString());
    }

    /**
     * Creates a successful response without data.
     */
    public static <T> ApiResponseDto<T> success(ApiCode code) {
        return new ApiResponseDto<>(true, code.getCode(), code.getMessage(), null, Instant.now().toString());
    }

    /**
     * Creates an error response without data.
     */
    public static <T> ApiResponseDto<T> error(ApiCode code) {
        return new ApiResponseDto<>(false, code.getCode(), code.getMessage(), null, Instant.now().toString());
    }

    /**
     * Creates an error response with custom message and data.
     */
    public static <T> ApiResponseDto<T> error(ApiCode code, String message, T data) {
        return new ApiResponseDto<>(false, code.getCode(), message, data, Instant.now().toString());
    }
}
