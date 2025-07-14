// File: src/main/java/com/example/sales/model/RefreshToken.java
package com.example.sales.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "refresh_tokens")
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken {
    @Id
    private String id;
    private String token;
    private String userId;
    private Date expiryDate;
}
