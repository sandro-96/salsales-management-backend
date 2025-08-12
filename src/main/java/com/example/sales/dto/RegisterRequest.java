// File: src/main/java/com/example/sales/dto/RegisterRequest.java
package com.example.sales.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @Email
    private String email;

    @Size(min = 6, message = "Mật khẩu phải từ 6 ký tự")
    private String password;

    @NotBlank(message = "Họ không được để trống")
    @Size(max = 50, message = "Họ không được vượt quá 50 ký tự")
    private String lastName;

    @NotBlank(message = "Tên không được để trống")
    @Size(max = 50, message = "Tên không được vượt quá 50 ký tự")
    private String firstName;

    @Size(max = 50, message = "Tên đệm không được vượt quá 50 ký tự")
    private String middleName;
}
