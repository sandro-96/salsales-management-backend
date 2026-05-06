// File: src/main/java/com/example/sales/dto/ChangePasswordRequest.java
package com.example.sales.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    /**
     * Bắt buộc khi tài khoản đã có mật khẩu đăng nhập bằng email.
     * Bỏ qua (để trống) nếu chỉ đăng nhập Google và đang đặt mật khẩu lần đầu.
     */
    private String currentPassword;

    @NotBlank
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    private String newPassword;
}