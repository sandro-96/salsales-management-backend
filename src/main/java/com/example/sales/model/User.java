// File: src/main/java/com/example/sales/model/User.java
package com.example.sales.model;

import com.example.sales.constant.UserRole;
import com.example.sales.model.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    private String password;

    private String businessType;
    private String fullName;
    private String cname;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String timezone;
    private String currency;
    private String language;
    private String countryCode;
    private String avatarUrl;

    @Builder.Default
    private boolean verified = false;

    private String verificationToken;
    @JsonIgnore
    private Date verificationExpiry;

    @Builder.Default
    private UserRole role = UserRole.ROLE_USER;

    @JsonIgnore
    private String resetToken;

    private Date resetTokenExpiry;
    private boolean active = true;
    private Date lastLoginAt;
    private Date birthDate;
    private String gender;

    private String internalNote;
}

