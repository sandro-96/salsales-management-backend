package com.example.sales.dto.user;

import com.example.sales.constant.Gender;
import com.example.sales.constant.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

@Data
@Builder
public class UserResponse {
    private String id;
    private String email;
    private String fullName;
    private String firstName;
    private String lastName;
    private String middleName;
    private String phone;
    private String city;
    private String state;
    private String avatarUrl;
    private String zipCode;
    private String timezone;
    private String currency;
    private String language;
    private String address;
    private String countryCode;
    private UserRole role;
    private boolean verified;
    private boolean active;
    private Instant lastLoginAt;
    private LocalDate birthDate;
    private Gender gender;
}
