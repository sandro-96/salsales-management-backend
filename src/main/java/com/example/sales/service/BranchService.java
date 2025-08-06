// File: src/main/java/com/example/sales/service/BranchService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.branch.BranchRequest;
import com.example.sales.dto.branch.BranchResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Branch;
import com.example.sales.repository.BranchRepository;
import com.example.sales.repository.ShopUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final AuditLogService auditLogService;
    private final ShopUserRepository shopUserRepository;
    private final MongoTemplate mongoTemplate;

    public Page<BranchResponse> getAll(String userId, String shopId, Boolean active, String keyword, Pageable pageable) {
        Query query = new Query();
        query.addCriteria(Criteria.where("shopId").is(shopId).and("deleted").is(false));

        if (active != null) {
            query.addCriteria(Criteria.where("active").is(active));
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String regex = ".*" + keyword.trim().toLowerCase() + ".*";
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("name").regex(regex, "i"),
                    Criteria.where("address").regex(regex, "i")
            ));
        }
        query.with(pageable);
        List<Branch> branches = mongoTemplate.find(query, Branch.class);
        return PageableExecutionUtils.getPage(branches, pageable, () ->
                mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Branch.class)
        ).map(this::toResponse);
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
