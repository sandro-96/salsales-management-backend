package com.example.sales.dto.staffProfile;

import com.example.sales.constant.ContractType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class StaffProfileRequest {
    private String fullName;
    private String phone;
    private String email;

    private String branchId;
    private String position;
    private String department;
    private String level;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
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
}
