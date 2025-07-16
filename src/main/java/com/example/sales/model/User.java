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

@Getter
@Setter
@ToString(exclude = {
        "password",
        "verificationToken",
        "resetToken"
}) // 👈 Tránh lộ thông tin nhạy cảm & vòng lặp
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "users")
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
    private String fullName;
    private String phone;
    private String avatarUrl;

    @Builder.Default
    private boolean verified = false;

    private String verificationToken;
    private Date verificationExpiry;

    @Builder.Default
    private UserRole role = UserRole.ROLE_USER;

    private String resetToken;
    private Date resetTokenExpiry;
}

