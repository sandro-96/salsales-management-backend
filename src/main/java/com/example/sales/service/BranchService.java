// File: src/main/java/com/example/sales/service/BranchService.java
package com.example.sales.service;

import com.example.sales.constant.ApiErrorCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.branch.BranchRequest;
import com.example.sales.dto.branch.BranchResponse;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Branch;
import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.repository.BranchRepository;
import com.example.sales.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final ShopRepository shopRepository;
    private final ShopUserService shopUserService;

    public List<BranchResponse> getAll(User user) {
        Shop shop = getShop(user);
        shopUserService.requireAnyRole(shop.getId(), user.getId(), ShopRole.OWNER);

        return branchRepository.findByShopId(shop.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BranchResponse create(User user, BranchRequest req) {
        Shop shop = getShop(user);
        shopUserService.requireOwner(shop.getId(), user.getId());

        Branch branch = Branch.builder()
                .shopId(shop.getId())
                .name(req.getName())
                .address(req.getAddress())
                .phone(req.getPhone())
                .active(req.isActive())
                .build();

        return toResponse(branchRepository.save(branch));
    }

    public BranchResponse update(User user, String id, BranchRequest req) {
        Shop shop = getShop(user);
        shopUserService.requireOwner(shop.getId(), user.getId());

        Branch branch = branchRepository.findById(id)
                .filter(b -> b.getShopId().equals(shop.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.BRANCH_NOT_FOUND));

        branch.setName(req.getName());
        branch.setAddress(req.getAddress());
        branch.setPhone(req.getPhone());
        branch.setActive(req.isActive());

        return toResponse(branchRepository.save(branch));
    }

    public void delete(User user, String id) {
        Shop shop = getShop(user);
        shopUserService.requireOwner(shop.getId(), user.getId());

        Branch branch = branchRepository.findById(id)
                .filter(b -> b.getShopId().equals(shop.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.BRANCH_NOT_FOUND));

        branchRepository.delete(branch);
    }

    private Shop getShop(User user) {
        return shopRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.SHOP_NOT_FOUND));
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
