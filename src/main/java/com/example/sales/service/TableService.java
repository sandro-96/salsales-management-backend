// File: src/main/java/com/example/sales/service/TableService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.TableStatus;
import com.example.sales.dto.TableRequest;
import com.example.sales.dto.TableResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Shop;
import com.example.sales.model.Table;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TableService {

    private final TableRepository tableRepository;
    private final ShopRepository shopRepository;
    private final AuditLogService auditLogService;

    public TableResponse create(TableRequest request) {
        String shopId = request.getShopId();
        String branchId = request.getBranchId();

        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        Table table = Table.builder()
                .name(request.getName())
                .shopId(shopId)
                .branchId(branchId)
                .status(Optional.ofNullable(request.getStatus()).orElse(TableStatus.AVAILABLE))
                .capacity(request.getCapacity())
                .note(request.getNote())
                .build();

        Table saved = tableRepository.save(table);
        auditLogService.log(null, shopId, saved.getId(), "TABLE", "CREATED",
                String.format("Tạo bàn: %s (Chi nhánh: %s)", saved.getName(), saved.getBranchId()));
        return toResponse(saved, shop);
    }

    public List<TableResponse> getByShop(String shopId, String branchId) {
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));

        return tableRepository.findByShopIdAndBranchIdAndDeletedFalse(shopId, branchId)
                .stream()
                .map(table -> toResponse(table, shop))
                .toList();
    }

    public TableResponse updateStatus(String tableId, TableStatus status) {
        Table table = tableRepository.findByIdAndDeletedFalse(tableId)
                .orElseThrow(() -> new BusinessException(ApiCode.TABLE_NOT_FOUND));

        table.setStatus(status);
        Table saved = tableRepository.save(table);
        auditLogService.log(null, table.getShopId(), saved.getId(), "TABLE", "STATUS_UPDATED",
                String.format("Cập nhật trạng thái bàn: %s → %s", table.getName(), status));
        return toResponse(saved);
    }

    private TableResponse toResponse(Table table) {
        Shop shop = shopRepository.findByIdAndDeletedFalse(table.getShopId()).orElse(null);

        return TableResponse.builder()
                .id(table.getId())
                .name(table.getName())
                .status(table.getStatus())
                .shopId(table.getShopId())
                .branchId(table.getBranchId())
                .shopName(shop != null ? shop.getName() : null)
                .capacity(table.getCapacity())
                .note(table.getNote())
                .currentOrderId(table.getCurrentOrderId())
                .build();
    }

    private TableResponse toResponse(Table table, Shop shop) {
        return TableResponse.builder()
                .id(table.getId())
                .name(table.getName())
                .status(table.getStatus())
                .shopId(shop.getId())
                .branchId(table.getBranchId())
                .shopName(shop.getName())
                .capacity(table.getCapacity())
                .note(table.getNote())
                .currentOrderId(table.getCurrentOrderId())
                .build();
    }
}
