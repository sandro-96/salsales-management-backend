package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("customers")
@Data
public class Customer extends BaseEntity {
    @Id
    private String id;

    private String userId; // Chủ sở hữu
    private String name;
    private String phone;
    private String email;
    private String address;
    private String note;
}
