// File: src/main/java/com/example/sales/dto/customer/CustomerSearchRequest.java
package com.example.sales.dto.customer;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CustomerSearchRequest {
    private String keyword = "";
    private int page = 0;
    private int size = 20;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String sortBy = "createdAt";
    private String sortDir = "desc";
}

