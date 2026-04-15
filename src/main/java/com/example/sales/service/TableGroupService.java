package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.tableGroup.TableGroupRequest;
import com.example.sales.dto.tableGroup.TableGroupResponse;
import com.example.sales.dto.tableGroup.TableGroupUpdateRequest;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Table;
import com.example.sales.model.TableGroup;
import com.example.sales.repository.TableGroupRepository;
import com.example.sales.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TableGroupService {
    private final TableGroupRepository tableGroupRepository;
    private final TableRepository tableRepository;
    private final AuditLogService auditLogService;
    private final ShopUserService shopUserService;

    @Transactional
    public TableGroupResponse create(String userId, String shopId, String branchId, TableGroupRequest request) {
        shopUserService.requireAnyRole(shopId, userId, ShopRole.OWNER, ShopRole.STAFF);
        if (!StringUtils.hasText(branchId)) throw new BusinessException(ApiCode.VALIDATION_ERROR);

        List<String> tableIds = normalizeIds(request.getTableIds());
        if (tableIds.size() < 2) throw new BusinessException(ApiCode.VALIDATION_ERROR);

        // Validate tables belong to shop/branch
        for (String tid : tableIds) {
            Table t = tableRepository.findByIdAndDeletedFalse(tid)
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND));
            if (!shopId.equals(t.getShopId()) || !branchId.equals(t.getBranchId())) {
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }
        }

        // Ensure a table belongs to at most one group
        for (String tid : tableIds) {
            List<TableGroup> existing = tableGroupRepository
                    .findByShopIdAndBranchIdAndDeletedFalseAndTableIdsContains(shopId, branchId, tid);
            if (existing != null && !existing.isEmpty()) {
                throw new BusinessException(ApiCode.DUPLICATE_DATA);
            }
        }

        TableGroup g = TableGroup.builder()
                .shopId(shopId)
                .branchId(branchId)
                .name(StringUtils.hasText(request.getName()) ? request.getName().trim() : null)
                .tableIds(tableIds)
                .build();
        TableGroup saved = tableGroupRepository.save(g);
        auditLogService.log(userId, shopId, saved.getId(), "TABLE_GROUP", "CREATED",
                "Tạo nhóm bàn (%s): %s".formatted(branchId, String.join(",", tableIds)));
        return toResponse(saved);
    }

    public List<TableGroupResponse> list(String userId, String shopId, String branchId) {
        shopUserService.requireAnyRole(shopId, userId, ShopRole.OWNER, ShopRole.STAFF);
        if (!StringUtils.hasText(branchId)) throw new BusinessException(ApiCode.VALIDATION_ERROR);
        return tableGroupRepository.findByShopIdAndBranchIdAndDeletedFalse(shopId, branchId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TableGroupResponse update(String userId, String shopId, String branchId, String id, TableGroupUpdateRequest request) {
        shopUserService.requireAnyRole(shopId, userId, ShopRole.OWNER, ShopRole.STAFF);
        TableGroup g = tableGroupRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.NOT_FOUND));
        if (!shopId.equals(g.getShopId()) || !branchId.equals(g.getBranchId())) {
            throw new ResourceNotFoundException(ApiCode.NOT_FOUND);
        }

        if (request.getName() != null) {
            g.setName(StringUtils.hasText(request.getName()) ? request.getName().trim() : null);
        }
        if (request.getTableIds() != null) {
            List<String> tableIds = normalizeIds(request.getTableIds());
            if (tableIds.size() < 2) throw new BusinessException(ApiCode.VALIDATION_ERROR);

            for (String tid : tableIds) {
                Table t = tableRepository.findByIdAndDeletedFalse(tid)
                        .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TABLE_NOT_FOUND));
                if (!shopId.equals(t.getShopId()) || !branchId.equals(t.getBranchId())) {
                    throw new BusinessException(ApiCode.VALIDATION_ERROR);
                }
            }
            // ensure not in other groups
            for (String tid : tableIds) {
                List<TableGroup> existing = tableGroupRepository
                        .findByShopIdAndBranchIdAndDeletedFalseAndTableIdsContains(shopId, branchId, tid);
                if (existing != null) {
                    for (TableGroup eg : existing) {
                        if (!eg.getId().equals(g.getId())) throw new BusinessException(ApiCode.DUPLICATE_DATA);
                    }
                }
            }
            g.setTableIds(tableIds);
        }

        TableGroup saved = tableGroupRepository.save(g);
        auditLogService.log(userId, shopId, id, "TABLE_GROUP", "UPDATED", "Cập nhật nhóm bàn");
        return toResponse(saved);
    }

    @Transactional
    public void delete(String userId, String shopId, String branchId, String id) {
        shopUserService.requireAnyRole(shopId, userId, ShopRole.OWNER, ShopRole.STAFF);
        TableGroup g = tableGroupRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.NOT_FOUND));
        if (!shopId.equals(g.getShopId()) || !branchId.equals(g.getBranchId())) {
            throw new ResourceNotFoundException(ApiCode.NOT_FOUND);
        }
        g.setDeleted(true);
        tableGroupRepository.save(g);
        auditLogService.log(userId, shopId, id, "TABLE_GROUP", "DELETED", "Giải nhóm bàn");
    }

    private TableGroupResponse toResponse(TableGroup g) {
        return TableGroupResponse.builder()
                .id(g.getId())
                .shopId(g.getShopId())
                .branchId(g.getBranchId())
                .name(g.getName())
                .tableIds(g.getTableIds())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }

    private static List<String> normalizeIds(List<String> ids) {
        if (ids == null) return List.of();
        Set<String> set = new HashSet<>();
        for (String x : ids) {
            if (!StringUtils.hasText(x)) continue;
            set.add(x.trim());
        }
        return new ArrayList<>(set);
    }
}

