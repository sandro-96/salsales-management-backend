package com.example.sales.dto.shopUser;

import com.example.sales.constant.Gender;
import com.example.sales.constant.ShopRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@Builder
public class ShopMemberResponse {
    private String id;
    private String fullName;
    private String email;
    private String address;
    private String city;
    private String state;
    private String phone;
    private String avatarUrl;
    private String userId;
    private LocalDate birthDate;
    private Gender gender;
    private ShopRole role;
    private LocalDateTime createdAt;
}
