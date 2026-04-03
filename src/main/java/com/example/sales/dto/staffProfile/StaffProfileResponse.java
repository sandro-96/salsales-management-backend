package com.example.sales.dto.staffProfile;

import com.example.sales.constant.ContractType;
import com.example.sales.constant.ShopRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class StaffProfileResponse {
    private String id;
    private String shopId;
    private String userId;
    private String branchId;

    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private ShopRole role;

    private String position;
    private String department;
    private String level;
    private LocalDate startDate;
    private Double salary;
    private ContractType contractType;

    private String idNumber;

    private String bankName;
    private String bankAccountNumber;
    private String bankAccountHolder;

    private String emergencyContactName;
    private String emergencyContactPhone;

    private String note;

    private boolean external;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
