// File: main/java/com/example/sales/exception/GlobalExceptionHandler.java
package com.example.sales.exception;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Xử lý lỗi validation cho các DTO
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        log.warn("Validation error at {}: {}", request.getDescription(false), errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.error(ApiCode.VALIDATION_ERROR, ApiCode.VALIDATION_ERROR.getMessage(), errors));
    }

    /**
     * Xử lý lỗi khi người dùng không có quyền truy cập
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseDto<String>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied at {}: {}", request.getDescription(false), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponseDto.error(ApiCode.ACCESS_DENIED, ApiCode.ACCESS_DENIED.getMessage(), ex.getMessage()));
    }

    /**
     * Xử lý lỗi kinh doanh
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponseDto<String>> handleBusinessException(
            BusinessException ex, WebRequest request) {
        log.error("Business error at {}: {} - {}",
                request.getDescription(false), ex.getError().getCode(), ex.getError().getMessage());
        return ResponseEntity
                .status(getHttpStatus(ex.getError()))
                .body(ApiResponseDto.error(ex.getError(), ex.getError().getMessage(), null));
    }

    /**
     * Xử lý các lỗi chung (fallback)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<String>> handleAllExceptions(
            Exception ex, WebRequest request) {
        request.getRemoteUser();
        log.error("Internal server error at {} for user {} (requestId: {}): {}",
                request.getDescription(false),
                request.getRemoteUser(),
                request.getHeader("X-Request-ID"),
                ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(ApiCode.INTERNAL_ERROR, ApiCode.INTERNAL_ERROR.getMessage(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDto<String>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid argument at {}: {}", request.getDescription(false), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.error(ApiCode.VALIDATION_ERROR, ApiCode.VALIDATION_ERROR.getMessage(), ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponseDto<String>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Invalid JSON at {}: {}", request.getDescription(false), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.error(ApiCode.VALIDATION_ERROR, ApiCode.VALIDATION_ERROR.getMessage(), ex.getMessage()));
    }

    /**
     * Ánh xạ ApiCode sang mã trạng thái HTTP
     */
    private HttpStatus getHttpStatus(ApiCode errorCode) {
        return switch (errorCode) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case NOT_FOUND, USER_NOT_FOUND, SHOP_NOT_FOUND, PRODUCT_NOT_FOUND, ORDER_NOT_FOUND,
                    TABLE_NOT_FOUND, BRANCH_NOT_FOUND, CUSTOMER_NOT_FOUND, PROMOTION_NOT_FOUND ->
                    HttpStatus.NOT_FOUND;
            case VALIDATION_ERROR, INVALID_TOKEN, ACCOUNT_LOCKED, REFRESH_TOKEN_EXPIRED,
                    REFRESH_TOKEN_INVALID, INCORRECT_PASSWORD, VALIDATION_FILE_ERROR,
                    PLAN_UPGRADE_REQUIRED, ORDER_ALREADY_PAID, INVALID_STATUS_TRANSITION,
                    PRODUCT_OUT_OF_STOCK, DUPLICATE_DATA, CANNOT_DELETE_SELF, SHOP_ALREADY_EXISTS,
                    EMAIL_EXISTS, ALREADY_VERIFIED, TOKEN_EXPIRED, EMAIL_NOT_VERIFIED ->
                    HttpStatus.BAD_REQUEST;
            case INTERNAL_ERROR, FILE_UPLOAD_FAILED, FILE_TYPE_NOT_ALLOWED ->
                    HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.OK;
        };
    }
}