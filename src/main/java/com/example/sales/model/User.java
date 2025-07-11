// File: src/main/java/com/example/sales/model/User.java
package com.example.sales.model;

import com.example.sales.constant.UserRole;
import com.example.sales.model.base.BaseEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;

@Builder
@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {
    @Id
    private String id;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    private String businessType;
    private String shopName;
    @Builder.Default
    private boolean verified = false; // Đã xác thực tài khoản chưa?
    private String verificationToken;
    private Date verificationExpiry;
    private UserRole role = UserRole.ROLE_USER; // Mặc định là USER
    private String resetToken; // Token dùng để reset password
    private Date resetTokenExpiry; // Thời hạn token
    private String fullName;
    private String phone;
    private String avatarUrl; // (nếu có ảnh đại diện)
}
