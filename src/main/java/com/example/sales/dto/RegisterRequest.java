// File: src/main/java/com/example/sales/dto/RegisterRequest.java
package com.example.sales.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @Email
    private String email;

    @Size(min = 6, message = "Mật khẩu phải từ 6 ký tự")
    private String password;
}
