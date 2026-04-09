package com.example.sales.service.tax;

import com.example.sales.constant.ApiCode;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.tax.TaxPolicy;
import com.example.sales.repository.TaxPolicyRepository;
import com.example.sales.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaxPolicyService {

    private final TaxPolicyRepository taxPolicyRepository;
    private final AuditLogService auditLogService;

    /**
     * Tìm tax policy đang hiệu lực (empty nếu shop chưa cấu hình).
     */
    public Optional<TaxPolicy> findEffectivePolicy(
            String shopId,
            String branchId,
            LocalDateTime atTime
    ) {
        LocalDateTime time = atTime != null ? atTime : LocalDateTime.now();

        if (branchId != null) {
            Optional<TaxPolicy> branchPolicy =
                    taxPolicyRepository.findEffectivePolicy(shopId, branchId, time);
            if (branchPolicy.isPresent()) {
                return branchPolicy;
            }
        }

        return taxPolicyRepository.findEffectivePolicy(shopId, null, time);
    }

    /**
     * Resolve tax policy đang hiệu lực cho shop / branch tại 1 thời điểm
     */
    public TaxPolicy resolveEffectivePolicy(
            String shopId,
            String branchId,
            LocalDateTime atTime
    ) {
        return findEffectivePolicy(shopId, branchId, atTime)
                .orElseThrow(() -> new BusinessException(ApiCode.TAX_POLICY_NOT_FOUND));
    }

    /**
     * Admin view: lấy toàn bộ policy của shop
     */
    public List<TaxPolicy> getAllPolicies(String shopId) {
        return taxPolicyRepository
                .findAllByShopIdOrderByEffectiveFromDesc(shopId);
    }

    /**
     * Soft-disable một policy (chỉ trong đúng cửa hàng)
     */
    public void deactivatePolicy(String userId, String shopId, String policyId) {
        TaxPolicy policy = taxPolicyRepository.findById(policyId)
                .orElseThrow(() -> new BusinessException(ApiCode.TAX_POLICY_NOT_FOUND));
        if (!Objects.equals(shopId, policy.getShopId())) {
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }

        policy.setActive(false);
        taxPolicyRepository.save(policy);

        auditLogService.log(
                userId,
                policy.getShopId(),
                policy.getId(),
                "TAX_POLICY",
                "DEACTIVATED",
                "Disable tax policy: " + policy.getName()
        );
    }

    /**
     * Tạo policy mới
     * (versioning & overlap do admin/business rule quyết)
     */
    public TaxPolicy createPolicy(String userId, TaxPolicy policy) {

        LocalDateTime from = policy.getEffectiveFrom() != null
                ? policy.getEffectiveFrom()
                : LocalDateTime.MIN;

        LocalDateTime to = policy.getEffectiveTo() != null
                ? policy.getEffectiveTo()
                : LocalDateTime.MAX;

        List<TaxPolicy> overlaps = taxPolicyRepository
                .findActiveOverlappingPolicies(
                        policy.getShopId(),
                        policy.getBranchId(),
                        from,
                        to
                );

        if (!overlaps.isEmpty()) {
            throw new BusinessException(ApiCode.TAX_POLICY_OVERLAP);
        }

        policy.setActive(true);
        TaxPolicy saved = taxPolicyRepository.save(policy);

        auditLogService.log(
                userId,
                policy.getShopId(),
                saved.getId(),
                "TAX_POLICY",
                "CREATED",
                "Tạo tax policy: " + saved.getName()
        );

        return saved;
    }
}
