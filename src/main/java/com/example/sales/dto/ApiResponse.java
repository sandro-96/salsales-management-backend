// File: src/main/java/com/example/sales/dto/ApiResponse.java
package com.example.sales.dto;

import com.example.sales.constant.ApiCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private ApiCode code;
    private T data;

    public static <T> ApiResponse<T> success(ApiCode code, T data) {
        return new ApiResponse<>(true, code, data);
    }

    public static <T> ApiResponse<T> success(ApiCode code) {
        return new ApiResponse<>(true, code, null);
    }

    public static ApiResponse<?> error(ApiCode code) {
        return new ApiResponse<>(false, code, null);
    }
}
