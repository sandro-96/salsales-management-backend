package com.example.sales.model;

import com.example.sales.constant.Gender;
import com.example.sales.constant.UserRole;
import com.example.sales.model.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.Instant;

@Getter
@Setter
@ToString(exclude = {"password", "verificationToken", "resetToken"})
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "users")
public class User extends BaseEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    @Email
    @NotBlank
    private String email;

    @NotBlank
    @JsonIgnore
    private String password;

    @NotBlank(message = "Họ không được để trống")
    @Size(max = 50, message = "Họ không được vượt quá 50 ký tự")
    private String lastName;

    @NotBlank(message = "Tên không được để trống")
    @Size(max = 50, message = "Tên không được vượt quá 50 ký tự")
    private String firstName;

    @Size(max = 50, message = "Tên đệm không được vượt quá 50 ký tự")
    private String middleName;

    private String phone;

    private String address;
    private String city;
    private String state;

    @Size(max = 10, message = "Mã bưu điện không được vượt quá 10 ký tự")
    private String zipCode;

    private String timezone;
    private String currency;
    private String language;
    private String countryCode;
    private String avatarUrl;

    @Builder.Default
    private boolean verified = false;

    @JsonIgnore
    private String verificationToken;
    private Instant verificationExpiry;

    @Builder.Default
    private UserRole role = UserRole.ROLE_USER;

    @JsonIgnore
    private String resetToken;
    private Instant resetTokenExpiry;

    @Builder.Default
    private boolean active = true;
    private Instant lastLoginAt;
    private LocalDate birthDate;

    private Gender gender;

    @Size(max = 500, message = "Ghi chú nội bộ không được vượt quá 500 ký tự")
    private String internalNote;

    @Indexed(unique = true, sparse = true)
    private String googleId;

    @JsonIgnore
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (lastName != null) {
            fullName.append(lastName);
        }
        if (middleName != null) {
            if (!fullName.isEmpty()) {
                fullName.append(" ");
            }
            fullName.append(middleName);
        }
        if (firstName != null) {
            if (!fullName.isEmpty()) {
                fullName.append(" ");
            }
            fullName.append(firstName);
        }
        String result = fullName.toString().trim();
        return result.isEmpty() ? null : result;
    }
}