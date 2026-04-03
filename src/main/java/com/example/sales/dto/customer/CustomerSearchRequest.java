package com.example.sales.dto.customer;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class CustomerSearchRequest {
    private String keyword = "";
    private int page = 0;
    private int size = 20;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    private String sortBy = "createdAt";
    private String sortDir = "desc";
}
