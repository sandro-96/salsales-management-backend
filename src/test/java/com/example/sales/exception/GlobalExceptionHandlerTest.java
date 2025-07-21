// File: src/test/java/com/example/sales/exception/GlobalExceptionHandlerTest.java
package com.example.sales.exception;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void testHandleValidationExceptions() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("objectName", "name", "Field is required");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        WebRequest webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("POST /api/users");

        ResponseEntity<ApiResponseDto<Map<String, String>>> response = handler.handleValidationExceptions(ex, webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(false, response.getBody().isSuccess());
        assertEquals("4000", response.getBody().getCode());
        assertEquals("Validation error", response.getBody().getMessage());
        assertEquals("Field is required", response.getBody().getData().get("name"));
    }

    @Test
    void testHandleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        WebRequest webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("GET /api/admin");

        ResponseEntity<ApiResponseDto<String>> response = handler.handleAccessDeniedException(ex, webRequest);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(false, response.getBody().isSuccess());
        assertEquals("4002", response.getBody().getCode());
        assertEquals("Access denied", response.getBody().getMessage());
        assertEquals("Access denied", response.getBody().getData());
    }

    @Test
    void testHandleBusinessException() {
        BusinessException ex = new BusinessException(ApiCode.USER_NOT_FOUND);
        WebRequest webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("GET /api/users/123");

        ResponseEntity<ApiResponseDto<String>> response = handler.handleBusinessException(ex, webRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(false, response.getBody().isSuccess());
        assertEquals("4108", response.getBody().getCode());
        assertEquals("User not found", response.getBody().getMessage());
        assertEquals(null, response.getBody().getData());
    }

    @Test
    void testHandleAllExceptions() {
        Exception ex = new RuntimeException("Unexpected error");
        WebRequest webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("GET /api/test");
        when(webRequest.getRemoteUser()).thenReturn("testUser");

        ResponseEntity<ApiResponseDto<String>> response = handler.handleAllExceptions(ex, webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(false, response.getBody().isSuccess());
        assertEquals("5000", response.getBody().getCode());
        assertEquals("Internal server error", response.getBody().getMessage());
        assertEquals("Unexpected error", response.getBody().getData());
    }

    @Test
    void testApiResponseSuccessWithData() {
        ApiResponseDto<String> response = ApiResponseDto.success(ApiCode.SUCCESS, "Test data");
        assertEquals(true, response.isSuccess());
        assertEquals("2000", response.getCode());
        assertEquals("Operation successful", response.getMessage());
        assertEquals("Test data", response.getData());
    }

    @Test
    void testApiResponseSuccessWithoutData() {
        ApiResponseDto<?> response = ApiResponseDto.success(ApiCode.SUCCESS);
        assertEquals(true, response.isSuccess());
        assertEquals("2000", response.getCode());
        assertEquals("Operation successful", response.getMessage());
        assertEquals(null, response.getData());
    }

    @Test
    void testApiResponseErrorWithoutData() {
        ApiResponseDto<?> response = ApiResponseDto.error(ApiCode.INTERNAL_ERROR);
        assertEquals(false, response.isSuccess());
        assertEquals("5000", response.getCode());
        assertEquals("Internal server error", response.getMessage());
        assertEquals(null, response.getData());
    }

    @Test
    void testApiResponseErrorWithCustomMessageAndData() {
        ApiResponseDto<String> response = ApiResponseDto.error(ApiCode.VALIDATION_ERROR, "Custom validation error", "Details");
        assertEquals(false, response.isSuccess());
        assertEquals("4000", response.getCode());
        assertEquals("Custom validation error", response.getMessage());
        assertEquals("Details", response.getData());
    }
}