// File: src/main/java/com/example/sales/dto/customer/CustomerRequest.java
package com.example.sales.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerRequest {
    @NotBlank(message = "Tên khách hàng không được để trống")
    private String name;

    private String phone;

    @Email(message = "Email không hợp lệ")
    private String email;

    private String address;

    private String note;
    private String branchId; // Có thể null nếu không phân biệt chi nhánh
}
