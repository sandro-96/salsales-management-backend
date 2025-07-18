// File: src/main/java/com/example/sales/service/TableService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.constant.TableStatus;
import com.example.sales.dto.table.TableRequest;
import com.example.sales.dto.table.TableResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Shop;
import com.example.sales.model.Table;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TableService {

    private final TableRepository tableRepository;
    private final ShopRepository shopRepository;
    private final AuditLogService auditLogService;
    private final ShopUserService shopUserService;

    @Transactional
    public TableResponse create(String userId, TableRequest request) {
        String shopId = request.getShopId();
        String branchId = request.getBranchId();

        // Kiểm tra quyền truy cập
        shopUserService.requireAnyRole(shopId, userId, ShopRole.OWNER);

        // Kiểm tra cửa hàng tồn tại
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.SHOP_NOT_FOUND));

        // Kiểm tra trùng lặp tên bàn
        if (tableRepository.findByShopIdAndBranchIdAndDeletedFalse(shopId, branchId)
                .stream().anyMatch(table -> table.getName().equalsIgnoreCase(request.getName()))) {
            throw new BusinessException(ApiCode.TABLE_NAME_EXISTS);
        }

        // Kiểm tra capacity hợp lệ
        if (request.getCapacity() != null && request.getCapacity() <= 0) {
            throw new BusinessException(ApiCode.INVALID_CAPACITY);
        }

        Table table = Table.builder()
                .name(request.getName())
                .shopId(shopId)
                .branchId(branchId)
                .status(Optional.ofNullable(request.getStatus()).orElse(TableStatus.AVAILABLE))
                .capacity(request.getCapacity())
                .note(request.getNote())
                .build();

        Table saved = tableRepository.save(table);
        auditLogService.log(userId, shopId, saved.getId(), "TABLE", "CREATED",
                String.format("Tạo bàn: %s (Chi nhánh: %s)", saved.getName(), saved.getBranchId()));
        return toResponse(saved, shop);
    }

    public Page<TableResponse> getByShop(String userId, String shopId, String branchId, Pageable pageable) {
        // Kiểm tra quyền truy cập
        shopUserService.requireAnyRole(shopId, userId, ShopRole.OWNER, ShopRole.STAFF);

        // Kiểm tra cửa hàng tồn tại
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.SHOP_NOT_FOUND));

        // Lấy danh sách bàn với phân trang
        return tableRepository.findByShopIdAndBranchIdAndDeletedFalse(shopId, branchId, pageable)
                .map(table -> toResponse(table, shop));
    }

    @Transactional
    public TableResponse updateStatus(String userId, String tableId, TableStatus status) {
        // Kiểm tra quyền truy cập
        shopUserService.requireAnyRole(null, userId, ShopRole.OWNER, ShopRole.STAFF);

        Table table = tableRepository.findByIdAndDeletedFalse(tableId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND));

        table.setStatus(status);
        Table saved = tableRepository.save(table);
        auditLogService.log(userId, table.getShopId(), saved.getId(), "TABLE", "STATUS_UPDATED",
                String.format("Cập nhật trạng thái bàn: %s → %s", table.getName(), status));
        return toResponse(saved, shopRepository.findByIdAndDeletedFalse(table.getShopId()).orElse(null));
    }

    @Transactional
    public TableResponse updateTable(String userId, String tableId, TableRequest request) {
        // Kiểm tra quyền truy cập
        shopUserService.requireAnyRole(request.getShopId(), userId, ShopRole.OWNER);

        Table table = tableRepository.findByIdAndDeletedFalse(tableId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND));

        // Kiểm tra bàn không đang được sử dụng
        if (table.getStatus() == TableStatus.OCCUPIED) {
            throw new BusinessException(ApiCode.TABLE_OCCUPIED);
        }

        // Kiểm tra cửa hàng tồn tại
        Shop shop = shopRepository.findByIdAndDeletedFalse(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.SHOP_NOT_FOUND));

        // Kiểm tra trùng lặp tên bàn
        if (!table.getName().equalsIgnoreCase(request.getName()) &&
                tableRepository.findByShopIdAndBranchIdAndDeletedFalse(request.getShopId(), request.getBranchId())
                        .stream().anyMatch(t -> t.getName().equalsIgnoreCase(request.getName()))) {
            throw new BusinessException(ApiCode.TABLE_NAME_EXISTS);
        }

        // Kiểm tra capacity hợp lệ
        if (request.getCapacity() != null && request.getCapacity() <= 0) {
            throw new BusinessException(ApiCode.INVALID_CAPACITY);
        }

        // Cập nhật thông tin bàn
        table.setName(request.getName());
        table.setShopId(request.getShopId());
        table.setBranchId(request.getBranchId());
        table.setCapacity(request.getCapacity());
        table.setNote(request.getNote());
        table.setStatus(Optional.ofNullable(request.getStatus()).orElse(TableStatus.AVAILABLE));

        Table saved = tableRepository.save(table);
        auditLogService.log(userId, table.getShopId(), saved.getId(), "TABLE", "UPDATED",
                String.format("Cập nhật thông tin bàn: %s", table.getName()));
        return toResponse(saved, shop);
    }

    @Transactional
    public void deleteTable(String userId, String tableId) {
        // Kiểm tra quyền truy cập
        shopUserService.requireAnyRole(null, userId, ShopRole.OWNER);

        Table table = tableRepository.findByIdAndDeletedFalse(tableId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND));

        // Kiểm tra bàn không đang được sử dụng
        if (table.getStatus() == TableStatus.OCCUPIED) {
            throw new BusinessException(ApiCode.TABLE_OCCUPIED);
        }

        table.setDeleted(true);
        tableRepository.save(table);
        auditLogService.log(userId, table.getShopId(), tableId, "TABLE", "DELETED",
                String.format("Xóa bàn: %s", table.getName()));
    }

    private TableResponse toResponse(Table table, Shop shop) {
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
}
