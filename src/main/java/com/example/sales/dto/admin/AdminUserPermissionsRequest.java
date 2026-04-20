// File: src/main/java/com/example/sales/dto/admin/AdminUserPermissionsRequest.java
package com.example.sales.dto.admin;

import com.example.sales.constant.AdminPermission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserPermissionsRequest {

    /** Set mới (có thể rỗng) — service sẽ ghi đè toàn bộ. */
    private Set<AdminPermission> permissions;

    /**
     * Nếu chỉ định một preset ({@link AdminPermission.Preset}) thì service sẽ
     * lấy từ bảng preset + union với {@code permissions} (nếu có).
     */
    private AdminPermission.Preset preset;
}
