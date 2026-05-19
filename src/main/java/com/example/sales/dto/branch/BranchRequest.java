// File: src/main/java/com/example/sales/dto/branch/BranchRequest.java
package com.example.sales.dto.branch;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class BranchRequest {

    @NotBlank(message = "Tên chi nhánh không được để trống")
    private String name;

    private String address;
    private String phone;

    /** Nhiều SĐT; nếu gửi thì ưu tiên hơn {@link #phone}. */
    private List<String> phones;

    private LocalDate openingDate;

    private LocalTime openingTime;
    private LocalTime closingTime;

    private String managerName;
    private String managerPhone;

    private Integer capacity;
    private String description;

    /** MST riêng chi nhánh; để trống = dùng MST cửa hàng */
    private String taxRegistrationNumber;

    /** Wi‑Fi khách — tuỳ chọn, có thể in trên hóa đơn / tem QR bàn */
    private String wifiSsid;
    private String wifiPassword;

    /** Chuyển khoản khách — tuỳ chọn, in trên tem QR bàn */
    private String paymentBankName;
    private String paymentAccountNumber;
    private String paymentAccountHolder;
    private String paymentTransferNote;

    /** Invoice/receipt language: {@code vi} or {@code en}. */
    private String invoiceLocale;

    private boolean active = true;
    private boolean isDefault = false;
}
