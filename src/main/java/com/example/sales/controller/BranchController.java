// File: src/main/java/com/example/sales/controller/BranchController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.branch.BranchRequest;
import com.example.sales.dto.branch.BranchResponse;
import com.example.sales.security.RequireRole;
import com.example.sales.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
@Validated
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<List<BranchResponse>> getAll(@RequestParam String shopId) {
        return ApiResponse.success(ApiCode.SUCCESS, branchService.getAll(shopId));
    }

    @PostMapping
    @RequireRole(ShopRole.OWNER)
    public ApiResponse<BranchResponse> create(@RequestParam String shopId,
                                              @RequestBody @Valid BranchRequest request) {
        return ApiResponse.success(ApiCode.SUCCESS, branchService.create(shopId, request));
    }

    @PutMapping("/{id}")
    @RequireRole(ShopRole.OWNER)
    public ApiResponse<BranchResponse> update(@RequestParam String shopId,
                                              @PathVariable String id,
                                              @RequestBody @Valid BranchRequest request) {
        return ApiResponse.success(ApiCode.SUCCESS, branchService.update(shopId, id, request));
    }

    @DeleteMapping("/{id}")
    @RequireRole(ShopRole.OWNER)
    public ApiResponse<?> delete(@RequestParam String shopId,
                                 @PathVariable String id) {
        branchService.delete(shopId, id);
        return ApiResponse.success(ApiCode.SUCCESS);
    }
}
