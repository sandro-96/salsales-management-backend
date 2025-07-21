// File: src/main/java/com/example/sales/service/BranchService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.branch.BranchRequest;
import com.example.sales.dto.branch.BranchResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Branch;
import com.example.sales.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final AuditLogService auditLogService;

    public Page<BranchResponse> getAll(String userId, String shopId, Pageable pageable) {
        return branchRepository.findByShopIdAndDeletedFalse(shopId, pageable)
                .map(this::toResponse);
    }

    public BranchResponse create(String userId, String shopId, BranchRequest req) {
        Branch branch = Branch.builder()
                .shopId(shopId)
                .name(req.getName())
                .address(req.getAddress())
                .phone(req.getPhone())
                .active(req.isActive())
                .build();

        Branch saved = branchRepository.save(branch);
        auditLogService.log(userId, shopId, saved.getId(), "BRANCH", "CREATED",
                String.format("Tạo chi nhánh: %s - %s", saved.getName(), saved.getAddress()));
        return toResponse(saved);
    }

    public BranchResponse update(String userId, String shopId, String id, BranchRequest req) {
        Branch branch = branchRepository.findByIdAndDeletedFalse(id)
                .filter(b -> b.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.BRANCH_NOT_FOUND));

        branch.setName(req.getName());
        branch.setAddress(req.getAddress());
        branch.setPhone(req.getPhone());
        branch.setActive(req.isActive());

        Branch saved = branchRepository.save(branch);
        auditLogService.log(userId, shopId, saved.getId(), "BRANCH", "UPDATED",
                String.format("Cập nhật chi nhánh: %s - %s", saved.getName(), saved.getAddress()));
        return toResponse(saved);
    }

    public void delete(String userId, String shopId, String id) {
        long branchCount = branchRepository.countByShopIdAndDeletedFalse(shopId);
        if (branchCount <= 1) {
            throw new BusinessException(ApiCode.CANNOT_DELETE_ONLY_BRANCH);
        }

        Branch branch = branchRepository.findByIdAndDeletedFalse(id)
                .filter(b -> b.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.BRANCH_NOT_FOUND));

        branch.setDeleted(true);
        branchRepository.save(branch);
        auditLogService.log(userId, shopId, branch.getId(), "BRANCH", "DELETED",
                String.format("Xoá mềm chi nhánh: %s - %s", branch.getName(), branch.getAddress()));
    }

    private BranchResponse toResponse(Branch branch) {
        return BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .active(branch.isActive())
                .createdAt(branch.getCreatedAt() != null ? branch.getCreatedAt().toString() : null)
                .build();
    }
}
