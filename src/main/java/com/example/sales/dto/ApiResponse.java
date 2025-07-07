package com.example.sales.dto;

import com.example.sales.constant.ApiErrorCode;
import com.example.sales.constant.ApiMessage;
import com.example.sales.util.MessageService;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Locale;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;

    // ✅ Success with i18n
    public static <T> ApiResponse<T> success(ApiMessage msg, T data, MessageService ms, Locale locale) {
        return new ApiResponse<>(true, msg.getCode(), ms.get(msg.getMessage(), locale), data);
    }

    public static ApiResponse<?> success(ApiMessage msg, MessageService ms, Locale locale) {
        return success(msg, null, ms, locale);
    }

    // ✅ Error with i18n
    public static ApiResponse<?> error(ApiErrorCode error, MessageService ms, Locale locale) {
        return new ApiResponse<>(false, error.getCode(), ms.get(error.getMessage(), locale), null);
    }
}
