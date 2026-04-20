// File: src/main/java/com/example/sales/dto/admin/AdminUserStatusUpdateRequest.java
package com.example.sales.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserStatusUpdateRequest {

    @NotNull
    private Boolean active;

    private String reason;
}
