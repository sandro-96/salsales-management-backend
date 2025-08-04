package com.example.sales.dto.shopUser;

import com.example.sales.constant.ShopRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Builder
public class ShopMemberResponse {
    private String id;
    private String fullName;
    private String cname;
    private String email;
    private String address;
    private String city;
    private String state;
    private String phone;
    private String avatarUrl;
    private String userId;
    private Date birthDate;
    private String gender;
    private ShopRole role;
    private LocalDateTime createdAt;
}
