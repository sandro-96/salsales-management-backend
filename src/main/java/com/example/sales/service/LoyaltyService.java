package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.PointTransactionType;
import com.example.sales.dto.customer.PointTransactionResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Customer;
import com.example.sales.model.PointTransaction;
import com.example.sales.repository.CustomerRepository;
import com.example.sales.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final CustomerRepository customerRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final AuditLogService auditLogService;

    /**
     * Tỷ lệ tích điểm mặc định: 1 điểm / 10.000₫ chi tiêu
     */
    private static final long POINTS_PER_UNIT = 1;
    private static final long AMOUNT_PER_POINT = 10_000;

    /**
     * Giá trị quy đổi: 1 điểm = 1.000₫
     */
    private static final long POINT_VALUE_VND = 1_000;

    /**
     * Tích điểm khi thanh toán đơn hàng
     */
    public void earnPoints(String shopId, String branchId, String customerId,
                           double orderAmount, String orderId, String userId) {
        Customer customer = getCustomer(shopId, customerId);

        long pointsEarned = (long) (orderAmount / AMOUNT_PER_POINT) * POINTS_PER_UNIT;
        if (pointsEarned <= 0) return;

        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + pointsEarned);
        customer.setTotalPointsEarned(customer.getTotalPointsEarned() + pointsEarned);
        customerRepository.save(customer);

        PointTransaction tx = PointTransaction.builder()
                .shopId(shopId)
                .branchId(branchId)
                .customerId(customerId)
                .type(PointTransactionType.EARN)
                .points(pointsEarned)
                .balanceAfter(customer.getLoyaltyPoints())
                .referenceId(orderId)
                .note("Tích điểm đơn hàng " + orderId)
                .build();
        pointTransactionRepository.save(tx);

        log.info("Customer {} earned {} points from order {}", customerId, pointsEarned, orderId);
        auditLogService.log(userId, shopId, customerId, "CUSTOMER", "POINTS_EARNED",
                "Tích %d điểm từ đơn hàng %s".formatted(pointsEarned, orderId));
    }

    /**
     * Đổi điểm (trừ vào đơn hàng)
     * @return Giá trị tiền được giảm (VNĐ)
     */
    public long redeemPoints(String shopId, String branchId, String customerId,
                             long pointsToRedeem, String orderId, String userId) {
        Customer customer = getCustomer(shopId, customerId);

        if (pointsToRedeem <= 0) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
        if (customer.getLoyaltyPoints() < pointsToRedeem) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        customer.setLoyaltyPoints(customer.getLoyaltyPoints() - pointsToRedeem);
        customer.setTotalPointsRedeemed(customer.getTotalPointsRedeemed() + pointsToRedeem);
        customerRepository.save(customer);

        PointTransaction tx = PointTransaction.builder()
                .shopId(shopId)
                .branchId(branchId)
                .customerId(customerId)
                .type(PointTransactionType.REDEEM)
                .points(-pointsToRedeem)
                .balanceAfter(customer.getLoyaltyPoints())
                .referenceId(orderId)
                .note("Đổi điểm cho đơn hàng " + orderId)
                .build();
        pointTransactionRepository.save(tx);

        long discountAmount = pointsToRedeem * POINT_VALUE_VND;

        log.info("Customer {} redeemed {} points for order {}", customerId, pointsToRedeem, orderId);
        auditLogService.log(userId, shopId, customerId, "CUSTOMER", "POINTS_REDEEMED",
                "Đổi %d điểm (-%s₫) cho đơn hàng %s".formatted(
                        pointsToRedeem, String.format("%,d", discountAmount), orderId));

        return discountAmount;
    }

    /**
     * Hoàn điểm khi hủy đơn hàng
     */
    public void refundPoints(String shopId, String branchId, String customerId,
                             String orderId, String userId) {
        var txList = pointTransactionRepository.findByReferenceIdAndShopId(orderId, shopId);

        long netPoints = txList.stream()
                .filter(t -> t.getCustomerId().equals(customerId))
                .mapToLong(PointTransaction::getPoints)
                .sum();

        if (netPoints == 0) return;

        Customer customer = getCustomer(shopId, customerId);

        // Hoàn lại: nếu net > 0 (đã earn) → trừ lại; nếu net < 0 (đã redeem) → cộng lại
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() - netPoints);
        if (netPoints > 0) {
            customer.setTotalPointsEarned(customer.getTotalPointsEarned() - netPoints);
        } else {
            customer.setTotalPointsRedeemed(customer.getTotalPointsRedeemed() + netPoints); // netPoints is negative
        }
        customerRepository.save(customer);

        PointTransaction tx = PointTransaction.builder()
                .shopId(shopId)
                .branchId(branchId)
                .customerId(customerId)
                .type(PointTransactionType.REFUND)
                .points(-netPoints)
                .balanceAfter(customer.getLoyaltyPoints())
                .referenceId(orderId)
                .note("Hoàn điểm do hủy đơn hàng " + orderId)
                .build();
        pointTransactionRepository.save(tx);

        log.info("Refunded {} points for customer {} from cancelled order {}", -netPoints, customerId, orderId);
    }

    /**
     * Điều chỉnh điểm thủ công (admin)
     */
    public void adjustPoints(String shopId, String customerId, long points,
                             String note, String userId) {
        Customer customer = getCustomer(shopId, customerId);

        long newBalance = customer.getLoyaltyPoints() + points;
        if (newBalance < 0) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }

        customer.setLoyaltyPoints(newBalance);
        if (points > 0) {
            customer.setTotalPointsEarned(customer.getTotalPointsEarned() + points);
        } else {
            customer.setTotalPointsRedeemed(customer.getTotalPointsRedeemed() + Math.abs(points));
        }
        customerRepository.save(customer);

        PointTransaction tx = PointTransaction.builder()
                .shopId(shopId)
                .customerId(customerId)
                .type(PointTransactionType.ADJUST)
                .points(points)
                .balanceAfter(newBalance)
                .note(note != null ? note : "Điều chỉnh thủ công")
                .build();
        pointTransactionRepository.save(tx);

        auditLogService.log(userId, shopId, customerId, "CUSTOMER", "POINTS_ADJUSTED",
                "Điều chỉnh %+d điểm. Số dư: %d".formatted(points, newBalance));
    }

    /**
     * Lấy lịch sử giao dịch điểm
     */
    public Page<PointTransactionResponse> getPointHistory(String shopId, String customerId,
                                                          int page, int size) {
        getCustomer(shopId, customerId); // validate
        return pointTransactionRepository
                .findByCustomerIdAndShopIdOrderByCreatedAtDesc(customerId, shopId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    /**
     * Lấy số dư điểm hiện tại
     */
    public long getBalance(String shopId, String customerId) {
        return getCustomer(shopId, customerId).getLoyaltyPoints();
    }

    /**
     * Tính giá trị tiền của điểm
     */
    public long calculatePointValue(long points) {
        return points * POINT_VALUE_VND;
    }

    private Customer getCustomer(String shopId, String customerId) {
        return customerRepository.findByIdAndDeletedFalse(customerId)
                .filter(c -> c.getShopId().equals(shopId))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.CUSTOMER_NOT_FOUND));
    }

    private PointTransactionResponse toResponse(PointTransaction tx) {
        return PointTransactionResponse.builder()
                .id(tx.getId())
                .type(tx.getType().name())
                .points(tx.getPoints())
                .balanceAfter(tx.getBalanceAfter())
                .referenceId(tx.getReferenceId())
                .note(tx.getNote())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
