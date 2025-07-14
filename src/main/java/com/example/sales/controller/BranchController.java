// File: src/main/java/com/example/sales/controller/BranchController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.branch.BranchRequest;
import com.example.sales.dto.branch.BranchResponse;
import com.example.sales.model.User;
import com.example.sales.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ApiResponse<List<BranchResponse>> getAll(@AuthenticationPrincipal User user) {
        return ApiResponse.success(ApiCode.SUCCESS, branchService.getAll(user));
    }

    @PostMapping
    public ApiResponse<BranchResponse> create(@AuthenticationPrincipal User user,
                                              @RequestBody @Valid BranchRequest request) {
        return ApiResponse.success(ApiCode.SUCCESS, branchService.create(user, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<BranchResponse> update(@AuthenticationPrincipal User user,
                                              @PathVariable String id,
                                              @RequestBody @Valid BranchRequest request) {
        return ApiResponse.success(ApiCode.SUCCESS, branchService.update(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@AuthenticationPrincipal User user,
                                 @PathVariable String id) {
        branchService.delete(user, id);
        return ApiResponse.success(ApiCode.SUCCESS);
    }
}
