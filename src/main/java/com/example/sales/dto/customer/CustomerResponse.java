// File: src/main/java/com/example/sales/dto/customer/CustomerResponse.java

package com.example.sales.dto.customer;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerResponse {
    private String id;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String note;
}
