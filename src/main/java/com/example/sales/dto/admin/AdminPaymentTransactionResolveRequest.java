// File: src/main/java/com/example/sales/dto/admin/AdminPaymentTransactionResolveRequest.java
package com.example.sales.dto.admin;

import com.example.sales.constant.PaymentTransactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload cho endpoint {@code POST /api/admin/billing/transactions/{id}/resolve}.
 * Chỉ chấp nhận {@code CANCELLED} hoặc {@code FAILED} — không cho set SUCCESS thủ công.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentTransactionResolveRequest {

    @NotNull(message = "Trạng thái mới không được để trống")
    private PaymentTransactionStatus status;

    @NotBlank(message = "Lý do không được để trống")
    @Size(max = 500, message = "Lý do không vượt quá 500 ký tự")
    private String reason;
}
