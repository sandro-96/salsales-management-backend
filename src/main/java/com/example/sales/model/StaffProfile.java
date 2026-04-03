package com.example.sales.model;

import com.example.sales.constant.ContractType;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("staff_profiles")
@CompoundIndex(name = "shop_user_idx", def = "{'shopId': 1, 'userId': 1}")
public class StaffProfile extends BaseEntity {

    @Id
    private String id;

    private String shopId;
    private String userId;
    private String branchId;

    private String fullName;
    private String phone;
    private String email;

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
}
