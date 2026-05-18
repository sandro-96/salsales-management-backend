// File: src/main/java/com/example/sales/dto/table/TableResponse.java
package com.example.sales.dto.table;

import com.example.sales.constant.TableStatus;
import lombok.*;

@Getter
@Setter
@Builder
public class TableResponse {
    private String id;
    private String name;
    private TableStatus status;
    private String shopId;
    private String shopName;
    private Integer capacity;
    private String note;
    private String currentOrderId;
    private String branchId; // Có thể null nếu không phân biệt chi nhánh

    /** Bàn “luôn trống” (vd: Mang đi): không chuyển sang OCCUPIED khi có đơn. */
    private Boolean alwaysAvailable;

    /** Token public dùng cho QR self-ordering tại bàn (UUID). */
    private String qrToken;

    /** Cho phép QR ordering ở bàn này (owner có thể tắt riêng từng bàn). */
    private boolean qrOrderingEnabled;

    /**
     * URL đầy đủ khách scan QR sẽ truy cập, dạng {@code <feUrl>/t/<shopSlug>/<qrToken>}.
     * Server compose sẵn để FE chỉ việc dùng (hiển thị / generate ảnh QR).
     */
    private String qrUrl;
}


