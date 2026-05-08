package com.example.sales.model;

import com.example.sales.model.base.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("attendance_logs")
@CompoundIndex(name = "shop_staff_date_idx", def = "{'shopId': 1, 'staffRef': 1, 'workDate': 1}")
public class AttendanceLog extends BaseEntity {

    @Id
    private String id;

    private String shopId;
    /** userId của hệ thống hoặc profileId của external staff. */
    private String staffRef;
    /** "SYSTEM" hoặc "EXTERNAL" (string để dễ migrate/extend). */
    private String staffType;

    private String branchId;

    private LocalDate workDate;

    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;

    private String note;

    /**
     * Phase 2+ (chi tiết hơn): nhiều session/ca trong cùng 1 ngày.
     * <p>
     * Giữ tương thích ngược với {@code checkInAt/checkOutAt}:
     * - Nếu {@code sessions} rỗng nhưng có checkInAt/checkOutAt, service sẽ coi đó là session #1.
     */
    @Builder.Default
    private List<AttendanceSession> sessions = new ArrayList<>();

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttendanceSession {
        private LocalDateTime checkInAt;
        private LocalDateTime checkOutAt;
        private String note;
    }
}

