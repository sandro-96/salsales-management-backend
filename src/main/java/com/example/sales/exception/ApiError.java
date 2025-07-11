// File: src/main/java/com/example/sales/exception/ApiError.java
package com.example.sales.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiError {
    private int status;
    private String message;
}