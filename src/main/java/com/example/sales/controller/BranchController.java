package com.example.sales.controller;

import com.example.sales.constant.ApiMessage;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.branch.BranchRequest;
import com.example.sales.dto.branch.BranchResponse;
import com.example.sales.model.User;
import com.example.sales.service.BranchService;
import com.example.sales.util.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;
    private final MessageService messageService;

    @GetMapping
    public ApiResponse<List<BranchResponse>> getAll(@AuthenticationPrincipal User user, Locale locale) {
        return ApiResponse.success(ApiMessage.SUCCESS, branchService.getAll(user), messageService, locale);
    }

    @PostMapping
    public ApiResponse<BranchResponse> create(@AuthenticationPrincipal User user,
                                              @RequestBody @Valid BranchRequest request,
                                              Locale locale) {
        return ApiResponse.success(ApiMessage.SUCCESS, branchService.create(user, request), messageService, locale);
    }

    @PutMapping("/{id}")
    public ApiResponse<BranchResponse> update(@AuthenticationPrincipal User user,
                                              @PathVariable String id,
                                              @RequestBody @Valid BranchRequest request,
                                              Locale locale) {
        return ApiResponse.success(ApiMessage.SUCCESS, branchService.update(user, id, request), messageService, locale);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@AuthenticationPrincipal User user,
                                    @PathVariable String id,
                                    Locale locale) {
        branchService.delete(user, id);
        return ApiResponse.success(ApiMessage.SUCCESS, messageService, locale);
    }
}
