package com.example.sales.controller.shop;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.tax.TaxPolicyCreateRequest;
import com.example.sales.model.tax.TaxPolicy;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequireRole;
import com.example.sales.service.tax.TaxPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shops/{shopId}/tax-policies")
@RequiredArgsConstructor
@Validated
public class TaxPolicyController {

    private final TaxPolicyService taxPolicyService;

    @GetMapping
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Danh sách chính sách thuế của cửa hàng")
    public ApiResponseDto<List<TaxPolicy>> listPolicies(
            @PathVariable String shopId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS, taxPolicyService.getAllPolicies(shopId));
    }

    @PostMapping
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Tạo chính sách thuế mới")
    public ApiResponseDto<TaxPolicy> createPolicy(
            @PathVariable String shopId,
            @Valid @RequestBody TaxPolicyCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        String branch = request.getBranchId() != null && !request.getBranchId().isBlank()
                ? request.getBranchId().trim()
                : null;

        TaxPolicy policy = TaxPolicy.builder()
                .shopId(shopId)
                .branchId(branch)
                .name(request.getName().trim())
                .priceIncludesTax(request.isPriceIncludesTax())
                .rules(request.toTaxRules())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .build();

        TaxPolicy saved = taxPolicyService.createPolicy(user.getId(), policy);
        return ApiResponseDto.success(ApiCode.SUCCESS, saved);
    }

    @PutMapping("/{policyId}/deactivate")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Vô hiệu hóa chính sách thuế")
    public ApiResponseDto<Void> deactivatePolicy(
            @PathVariable String shopId,
            @PathVariable String policyId,
            @AuthenticationPrincipal CustomUserDetails user) {
        taxPolicyService.deactivatePolicy(user.getId(), shopId, policyId);
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }
}
