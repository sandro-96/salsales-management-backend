// File: src/main/java/com/example/sales/service/BranchService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.branch.BranchDetailResponse;
import com.example.sales.dto.branch.BranchListResponse;
import com.example.sales.dto.branch.BranchRequest;
import com.example.sales.dto.branch.BranchResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Branch;
import com.example.sales.repository.BranchRepository;
import com.example.sales.repository.ShopUserRepository;
import com.example.sales.util.SlugUtils;
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
    private final MongoTemplate mongoTemplate;


    public List<BranchListResponse> getAll(String shopId) {
        List<Branch> branches = branchRepository.findAllByShopIdAndDeletedFalse(shopId);
        return branches.stream().map(this::toListResponse).toList();
    }

    public BranchResponse create(String userId, String shopId, BranchRequest req) {
        Branch branch = Branch.builder()
                .shopId(shopId)
                .name(req.getName())
                .address(req.getAddress())
                .phone(req.getPhone())
                .openingDate(req.getOpeningDate())
                .openingTime(req.getOpeningTime())
                .closingTime(req.getClosingTime())
                .managerName(req.getManagerName())
                .managerPhone(req.getManagerPhone())
                .capacity(req.getCapacity())
                .description(req.getDescription())
                .active(req.isActive())
                .isDefault(req.isDefault())
                .slug(generateUniqueBranchSlug(
                        shopId,
                        req.getName()
                ))
                .build();

        Branch saved = branchRepository.save(branch);

        auditLogService.log(
                userId,
                shopId,
                saved.getId(),
                "BRANCH",
                "CREATED",
                String.format("Tạo chi nhánh: %s - %s", saved.getName(), saved.getAddress())
        );

        return toResponse(saved);
    }

    public BranchResponse update(String userId, String shopId, String id, BranchRequest req) {
        Branch branch = branchRepository.findByIdAndDeletedFalse(id)
                .filter(b -> b.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.BRANCH_NOT_FOUND));

        branch.setName(req.getName());
        branch.setAddress(req.getAddress());
        branch.setPhone(req.getPhone());
        branch.setOpeningDate(req.getOpeningDate());
        branch.setOpeningTime(req.getOpeningTime());
        branch.setClosingTime(req.getClosingTime());
        branch.setManagerName(req.getManagerName());
        branch.setManagerPhone(req.getManagerPhone());
        branch.setCapacity(req.getCapacity());
        branch.setDescription(req.getDescription());
        branch.setActive(req.isActive());

        Branch saved = branchRepository.save(branch);

        auditLogService.log(
                userId,
                shopId,
                saved.getId(),
                "BRANCH",
                "UPDATED",
                String.format("Cập nhật chi nhánh: %s - %s", saved.getName(), saved.getAddress())
        );

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

    public BranchDetailResponse getById(String id) {
        Branch branch = branchRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.BRANCH_NOT_FOUND));

        return toDetailResponse(branch);
    }

    public BranchDetailResponse getBySlug(String shopId, String slug) {
        Branch branch = branchRepository.findByShopIdAndSlugAndDeletedFalse(shopId, slug)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.BRANCH_NOT_FOUND));

        return toDetailResponse(branch);
    }

    public String generateUniqueBranchSlug(String shopId, String name) {
        String baseSlug = SlugUtils.toSlug(name);
        String slug = baseSlug;
        int counter = 1;

        while (branchRepository.existsByShopIdAndSlugAndDeletedFalse(shopId, slug)) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }


    private BranchResponse toResponse(Branch branch) {
        return BranchResponse.builder()
                .id(branch.getId())
                .slug(branch.getSlug())
                .shopId(branch.getShopId())
                .name(branch.getName())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .openingDate(branch.getOpeningDate())
                .openingTime(branch.getOpeningTime())
                .closingTime(branch.getClosingTime())
                .managerName(branch.getManagerName())
                .managerPhone(branch.getManagerPhone())
                .capacity(branch.getCapacity())
                .description(branch.getDescription())
                .active(branch.isActive())
                .isDefault(branch.isDefault())
                .createdAt(branch.getCreatedAt())
                .updatedAt(branch.getUpdatedAt())
                .build();
    }
    private BranchListResponse toListResponse(Branch branch) {
        return BranchListResponse.builder()
                .id(branch.getId())
                .slug(branch.getSlug())
                .name(branch.getName())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .active(branch.isActive())
                .isDefault(branch.isDefault())
                .build();
    }

    private BranchDetailResponse toDetailResponse(Branch branch) {
        return BranchDetailResponse.builder()
                .id(branch.getId())
                .shopId(branch.getShopId())
                .slug(branch.getSlug())
                .name(branch.getName())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .openingDate(branch.getOpeningDate())
                .openingTime(branch.getOpeningTime())
                .closingTime(branch.getClosingTime())
                .managerName(branch.getManagerName())
                .managerPhone(branch.getManagerPhone())
                .capacity(branch.getCapacity())
                .description(branch.getDescription())
                .active(branch.isActive())
                .isDefault(branch.isDefault())
                .createdAt(branch.getCreatedAt())
                .updatedAt(branch.getUpdatedAt())
                .build();
    }
}
