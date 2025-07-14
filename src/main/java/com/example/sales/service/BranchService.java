// File: src/main/java/com/example/sales/service/BranchService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.branch.BranchRequest;
import com.example.sales.dto.branch.BranchResponse;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Branch;
import com.example.sales.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    public List<BranchResponse> getAll(String shopId) {
        return branchRepository.findByShopId(shopId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BranchResponse create(String shopId, BranchRequest req) {
        Branch branch = Branch.builder()
                .shopId(shopId)
                .name(req.getName())
                .address(req.getAddress())
                .phone(req.getPhone())
                .active(req.isActive())
                .build();

        return toResponse(branchRepository.save(branch));
    }

    public BranchResponse update(String shopId, String id, BranchRequest req) {
        Branch branch = branchRepository.findById(id)
                .filter(b -> b.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.BRANCH_NOT_FOUND));

        branch.setName(req.getName());
        branch.setAddress(req.getAddress());
        branch.setPhone(req.getPhone());
        branch.setActive(req.isActive());

        return toResponse(branchRepository.save(branch));
    }

    public void delete(String shopId, String id) {
        Branch branch = branchRepository.findById(id)
                .filter(b -> b.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.BRANCH_NOT_FOUND));

        branchRepository.delete(branch);
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
