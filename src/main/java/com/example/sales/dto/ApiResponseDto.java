// File: src/main/java/com/example/sales/dto/ApiResponseDto.java
package com.example.sales.dto;

import com.example.sales.constant.ApiCode;
import com.example.sales.util.ApiMessages;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Generic API response. {@code messageKey} + {@code messageParams} are stable for clients;
 * {@code message} is resolved for the request locale (fallback for legacy clients).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDto<T> {
    private boolean success;
    private String code;
    private String message;
    private String messageKey;
    private Map<String, Object> messageParams;
    private T data;
    private String timestamp;

    public static <T> ApiResponseDto<T> success(ApiCode code, T data) {
        return build(true, code, data, null, null);
    }

    public static <T> ApiResponseDto<T> success(ApiCode code) {
        return build(true, code, null, null, null);
    }

    public static <T> ApiResponseDto<T> error(ApiCode code) {
        return build(false, code, null, null, null);
    }

  /** Error with extra payload (e.g. validation field map). */
    public static <T> ApiResponseDto<T> error(ApiCode code, T data) {
        return build(false, code, data, null, null);
    }

    public static <T> ApiResponseDto<T> error(ApiCode code, Map<String, Object> messageParams, T data) {
        return build(false, code, data, messageParams, null);
    }

    /** Error with a custom message override (detail text). */
    public static <T> ApiResponseDto<T> errorWithDetail(ApiCode code, String detail) {
        return build(false, code, null, null, detail);
    }

    public static <T> ApiResponseDto<T> errorWithDetail(ApiCode code, String detail, T data) {
        return build(false, code, data, null, detail);
    }

    private static <T> ApiResponseDto<T> build(
            boolean success,
            ApiCode code,
            T data,
            Map<String, Object> messageParams,
            String messageOverride) {
        String resolved = messageOverride != null
                ? messageOverride
                : ApiMessages.resolve(code, messageParams);
        return new ApiResponseDto<>(
                success,
                code.getCode(),
                resolved,
                code.getMessageKey(),
                messageParams,
                data,
                Instant.now().toString());
    }
}
