// File: src/main/java/com/example/sales/model/User.java
package com.example.sales.model;

import com.example.sales.constant.UserRole;
import com.example.sales.model.base.BaseEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Builder
@Document(collection = "users")
@AllArgsConstructor
@NoArgsConstructor
@Data
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
