// File: src/main/java/com/example/sales/service/TableService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.constant.TableStatus;
import com.example.sales.constant.WebSocketMessageType;
import com.example.sales.dto.table.TableRequest;
import com.example.sales.dto.table.TableResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Shop;
import com.example.sales.model.Table;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.TableRepository;
import com.example.sales.service.realtime.RealtimeEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TableService {

    private final TableRepository tableRepository;
    private final ShopRepository shopRepository;
    private final AuditLogService auditLogService;
    private final ShopUserService shopUserService;
    private final BranchAccessService branchAccessService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    @Value("${app.fe.url:}")
    private String feUrl;

    @Transactional
    public TableResponse create(String userId, TableRequest request) {
        String shopId = request.getShopId();
        String branchId = branchAccessService.effectiveBranchFilter(
                shopId, userId, request.getBranchId());
        request.setBranchId(branchId);

        // Kiểm tra quyền truy cập
        shopUserService.requireAnyRole(shopId, userId, ShopRole.OWNER, ShopRole.MANAGER);

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
                .alwaysAvailable(Boolean.TRUE.equals(request.getAlwaysAvailable()))
                .qrToken(generateQrToken())
                .qrOrderingEnabled(request.getQrOrderingEnabled() == null
                        || Boolean.TRUE.equals(request.getQrOrderingEnabled()))
                .build();

        Table saved = tableRepository.save(table);
        auditLogService.log(userId, shopId, saved.getId(), "TABLE", "CREATED",
                String.format("Tạo bàn: %s (Chi nhánh: %s)", saved.getName(), saved.getBranchId()));
        TableResponse resp = toResponse(saved, shop);
        publishTable(saved, resp, WebSocketMessageType.TABLE_CREATED);
        return resp;
    }

    public Page<TableResponse> getByShop(String userId, String shopId, String branchId, Pageable pageable) {
        // Kiểm tra quyền truy cập
        shopUserService.requireAnyRole(shopId, userId, ShopRole.OWNER, ShopRole.MANAGER, ShopRole.STAFF, ShopRole.CASHIER);
        branchId = branchAccessService.effectiveBranchFilter(shopId, userId, branchId);
        if (!StringUtils.hasText(branchId)) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        // Kiểm tra cửa hàng tồn tại
        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.SHOP_NOT_FOUND));

        // Lấy danh sách bàn với phân trang
        return tableRepository.findByShopIdAndBranchIdAndDeletedFalse(shopId, branchId, pageable)
                .map(table -> toResponse(table, shop));
    }

    public String getCurrentOrderId(String userId, String shopId, String tableId) {
        // Kiểm tra quyền truy cập
        shopUserService.requireAnyRole(shopId, userId, ShopRole.OWNER, ShopRole.MANAGER, ShopRole.STAFF, ShopRole.CASHIER);

        Table table = tableRepository.findByIdAndDeletedFalse(tableId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND));
        if (!shopId.equals(table.getShopId())) {
            throw new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND);
        }
        branchAccessService.assertBranchAccess(shopId, userId, table.getBranchId());
        // Bàn “luôn trống”: không có khái niệm “một đơn gắn bàn” — không dùng currentOrderId để resume.
        if (Boolean.TRUE.equals(table.getAlwaysAvailable())) {
            return null;
        }
        return StringUtils.hasText(table.getCurrentOrderId()) ? table.getCurrentOrderId().trim() : null;
    }

    /**
     * Clear currentOrderId for a table (self-heal) if it still points to a closed order.
     * This is intentionally idempotent.
     */
    @Transactional
    public void clearCurrentOrderIfMatches(String userId, String shopId, String tableId, String expectedOrderId) {
        shopUserService.requireAnyRole(shopId, userId, ShopRole.OWNER, ShopRole.MANAGER, ShopRole.STAFF, ShopRole.CASHIER);
        if (!StringUtils.hasText(expectedOrderId)) return;

        Table table = tableRepository.findByIdAndDeletedFalse(tableId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND));
        if (!shopId.equals(table.getShopId())) {
            throw new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND);
        }

        String current = StringUtils.hasText(table.getCurrentOrderId()) ? table.getCurrentOrderId().trim() : null;
        if (current != null && current.equals(expectedOrderId.trim())) {
            table.setCurrentOrderId(null);
            // Defensive: if order is closed, the table should be available.
            table.setStatus(TableStatus.AVAILABLE);
            Table saved = tableRepository.save(table);
            publishTable(saved, toResponse(saved, null), WebSocketMessageType.TABLE_STATUS_CHANGED);
        }
    }

    @Transactional
    public TableResponse updateStatus(String userId, String shopId, String tableId, TableStatus status) {
        // Kiểm tra quyền truy cập
        shopUserService.requireAnyRole(shopId, userId, ShopRole.OWNER, ShopRole.MANAGER, ShopRole.STAFF, ShopRole.CASHIER);

        Table table = tableRepository.findByIdAndDeletedFalse(tableId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND));
        if (!shopId.equals(table.getShopId())) {
            throw new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND);
        }
        branchAccessService.assertBranchAccess(shopId, userId, table.getBranchId());

        table.setStatus(status);
        Table saved = tableRepository.save(table);
        auditLogService.log(userId, table.getShopId(), saved.getId(), "TABLE", "STATUS_UPDATED",
                String.format("Cập nhật trạng thái bàn: %s → %s", table.getName(), status));
        TableResponse resp = toResponse(saved,
                shopRepository.findByIdAndDeletedFalse(table.getShopId()).orElse(null));
        publishTable(saved, resp, WebSocketMessageType.TABLE_STATUS_CHANGED);
        return resp;
    }

    @Transactional
    public TableResponse updateTable(String userId, String tableId, TableRequest request) {
        // Kiểm tra quyền truy cập
        shopUserService.requireAnyRole(request.getShopId(), userId, ShopRole.OWNER, ShopRole.MANAGER);

        Table table = tableRepository.findByIdAndDeletedFalse(tableId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND));
        branchAccessService.assertBranchAccess(request.getShopId(), userId, table.getBranchId());

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
        table.setAlwaysAvailable(Boolean.TRUE.equals(request.getAlwaysAvailable()));
        if (request.getQrOrderingEnabled() != null) {
            table.setQrOrderingEnabled(Boolean.TRUE.equals(request.getQrOrderingEnabled()));
        }
        if (!StringUtils.hasText(table.getQrToken())) {
            table.setQrToken(generateQrToken());
        }

        Table saved = tableRepository.save(table);
        auditLogService.log(userId, table.getShopId(), saved.getId(), "TABLE", "UPDATED",
                String.format("Cập nhật thông tin bàn: %s", table.getName()));
        TableResponse resp = toResponse(saved, shop);
        publishTable(saved, resp, WebSocketMessageType.TABLE_UPDATED);
        return resp;
    }

    @Transactional
    public void deleteTable(String userId, String shopId, String tableId) {
        // Kiểm tra quyền truy cập
        shopUserService.requireAnyRole(shopId, userId, ShopRole.OWNER, ShopRole.MANAGER);

        Table table = tableRepository.findByIdAndDeletedFalse(tableId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND));
        if (!shopId.equals(table.getShopId())) {
            throw new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND);
        }
        branchAccessService.assertBranchAccess(shopId, userId, table.getBranchId());

        // Kiểm tra bàn không đang được sử dụng
        if (table.getStatus() == TableStatus.OCCUPIED) {
            throw new BusinessException(ApiCode.TABLE_OCCUPIED);
        }

        table.setDeleted(true);
        Table saved = tableRepository.save(table);
        auditLogService.log(userId, table.getShopId(), tableId, "TABLE", "DELETED",
                String.format("Xóa bàn: %s", table.getName()));
        publishTable(saved, toResponse(saved, null), WebSocketMessageType.TABLE_DELETED);
    }

    /**
     * Tạo lại {@code qrToken} cho bàn — token cũ ngừng hoạt động ngay.
     */
    @Transactional
    public TableResponse regenerateQrToken(String userId, String shopId, String tableId) {
        shopUserService.requireAnyRole(shopId, userId, ShopRole.OWNER, ShopRole.MANAGER);

        Table table = tableRepository.findByIdAndDeletedFalse(tableId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND));
        if (!shopId.equals(table.getShopId())) {
            throw new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND);
        }
        branchAccessService.assertBranchAccess(shopId, userId, table.getBranchId());
        table.setQrToken(generateQrToken());
        Table saved = tableRepository.save(table);

        auditLogService.log(userId, shopId, saved.getId(), "TABLE", "QR_TOKEN_REGENERATED",
                String.format("Tạo lại QR token cho bàn: %s", saved.getName()));

        Shop shop = shopRepository.findByIdAndDeletedFalse(shopId).orElse(null);
        TableResponse resp = toResponse(saved, shop);
        publishTable(saved, resp, WebSocketMessageType.TABLE_UPDATED);
        return resp;
    }

    private void publishTable(Table table, TableResponse payload, WebSocketMessageType type) {
        if (table == null) return;
        realtimeEventPublisher.publishTableEvent(table.getShopId(), table.getBranchId(), type, payload);
    }

    private TableResponse toResponse(Table table, Shop shop) {
        boolean always = Boolean.TRUE.equals(table.getAlwaysAvailable());
        return TableResponse.builder()
                .id(table.getId())
                .name(table.getName())
                .status(table.getStatus())
                .shopId(table.getShopId())
                .branchId(table.getBranchId())
                .shopName(shop != null ? shop.getName() : null)
                .capacity(table.getCapacity())
                .note(table.getNote())
                .currentOrderId(always ? null : table.getCurrentOrderId())
                .alwaysAvailable(always)
                .qrToken(table.getQrToken())
                .qrOrderingEnabled(table.isQrOrderingEnabled())
                .qrUrl(buildQrUrl(shop, table))
                .build();
    }

    private String buildQrUrl(Shop shop, Table table) {
        if (shop == null || !StringUtils.hasText(table.getQrToken()) || !StringUtils.hasText(shop.getSlug())) {
            return null;
        }
        String base = feUrl == null ? "" : feUrl.replaceAll("/+$", "");
        return base + "/t/" + shop.getSlug() + "/" + table.getQrToken();
    }

    private String generateQrToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
