// File: src/main/java/com/example/sales/model/Table.java
package com.example.sales.model;

import com.example.sales.constant.TableStatus;
import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "tables")
public class Table extends BaseEntity {
    @Id
    private String id;

    private String shopId;
    private String branchId;
    private String name;
    private TableStatus status;

    private Integer capacity;
    private String note;
    private String currentOrderId;

    /**
     * Bàn “luôn trống” (vd: Mang đi): không chuyển sang OCCUPIED, không dùng {@link #currentOrderId}
     * để trỏ đơn — cho phép nhiều đơn cùng {@code tableId} song song (POS quản lý theo tab/đơn).
     */
    private Boolean alwaysAvailable;

    /**
     * Token public (UUID) dùng cho QR self-ordering tại bàn. URL khách quét dạng
     * {@code /t/{shopSlug}/{qrToken}} → guest có thể xem menu và tạo đơn IN_STORE.
     * Có thể revoke bằng cách regenerate (qrToken mới); token cũ sẽ ngừng hoạt động.
     */
    private String qrToken;

    /**
     * Cho phép QR ordering trên bàn này. Owner có thể tắt riêng từng bàn (vd bàn VIP,
     * bàn đang sửa chữa) mà không ảnh hưởng các bàn khác.
     */
    @Builder.Default
    private boolean qrOrderingEnabled = true;
}

