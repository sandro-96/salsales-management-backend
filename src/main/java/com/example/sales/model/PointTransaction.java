package com.example.sales.model;

import com.example.sales.constant.PointTransactionType;
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
@Document("point_transactions")
public class PointTransaction extends BaseEntity {

    @Id
    private String id;

    private String shopId;
    private String branchId;
    private String customerId;

    private PointTransactionType type;

    private long points;           // Số điểm thay đổi (dương = cộng, âm = trừ)

    private long balanceAfter;     // Số dư sau giao dịch

    private String referenceId;    // ID đơn hàng hoặc promotion

    private String note;
}
