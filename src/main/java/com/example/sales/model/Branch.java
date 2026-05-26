// File: src/main/java/com/example/sales/model/Branch.java
package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("branches")
public class Branch extends BaseEntity {

    @Id
    private String id;

    private String shopId;
    private String slug;
    private String name;
    private String address;
    /** SĐT chính (đồng bộ với phần tử đầu của {@link #phones}). */
    private String phone;
    /** Danh sách SĐT liên hệ chi nhánh. */
    private List<String> phones;
    private LocalDate openingDate;

    private LocalTime openingTime;
    private LocalTime closingTime;

    private String managerName;
    private String managerPhone;

    private Integer capacity;

    private String description;

    /**
     * MST riêng của chi nhánh (tùy chọn). Để trống = dùng MST của cửa hàng khi hiển thị/hóa đơn.
     */
    private String taxRegistrationNumber;

    /** Wi‑Fi phục vụ khách tại điểm bán — có thể in trên hóa đơn / tem QR bàn */
    private String wifiSsid;
    private String wifiPassword;

    /** Thông tin chuyển khoản khách (in trên tem QR bàn) */
    private String paymentBankName;
    private String paymentAccountNumber;
    private String paymentAccountHolder;
    /** Gợi ý nội dung CK, vd "Thanh toan" */
    private String paymentTransferNote;
    /** Ảnh QR chuyển khoản riêng của chi nhánh, hiển thị cho khách trên màn hình phụ. */
    private String paymentQrImageUrl;

    /** Ngôn ngữ in hóa đơn / bill tại chi nhánh: {@code vi} hoặc {@code en}. */
    @Builder.Default
    private String invoiceLocale = "vi";

    @Builder.Default
    private boolean isDefault = false;
    @Builder.Default
    private boolean active = true;
}

