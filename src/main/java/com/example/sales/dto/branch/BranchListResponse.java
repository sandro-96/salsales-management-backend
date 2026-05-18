// File: src/main/java/com/example/sales/dto/branch/BranchListResponse.java
package com.example.sales.dto.branch;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BranchListResponse {

    private String id;
    private String slug;
    private String name;
    private String address;
    private String phone;

    private String wifiSsid;
    private String wifiPassword;

    private String paymentBankName;
    private String paymentAccountNumber;
    private String paymentAccountHolder;
    private String paymentTransferNote;

    private String invoiceLocale;

    private boolean active;
    private boolean isDefault;
}
