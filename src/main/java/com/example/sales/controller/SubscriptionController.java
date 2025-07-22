// File: src/main/java/com/example/sales/controller/SubscriptionController.java
package com.example.sales.controller;

import com.example.sales.cache.ShopCache;
import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.subscription.UpgradePlanRequest;
import com.example.sales.model.Shop;
import com.example.sales.model.SubscriptionHistory;
import com.example.sales.repository.SubscriptionHistoryRepository;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.service.PaymentService;
import com.example.sales.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final ShopService shopService;
    private final ShopCache shopCache;
    private final PaymentService paymentService;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin gói hiện tại", description = "Trả về gói dịch vụ hiện tại của cửa hàng do người dùng sở hữu")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin cửa hàng và gói hiện tại thành công"),
            @ApiResponse(responseCode = "401", description = "Không xác thực hoặc token không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ApiResponseDto<Shop> getCurrentPlan(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user) {
        Shop shop = shopCache.getShopByOwner(user.getId());
        return ApiResponseDto.success(ApiCode.SUCCESS, shop);
    }

    @PostMapping("/upgrade")
    @Operation(summary = "Nâng cấp gói dịch vụ", description = "Nâng cấp cửa hàng lên gói dịch vụ cao hơn (Pro, Enterprise)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nâng cấp gói thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ hoặc gói không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Không xác thực hoặc token không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ApiResponseDto<?> upgrade(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user,
            @RequestBody @Valid @Parameter(description = "Thông tin nâng cấp gói dịch vụ") UpgradePlanRequest req) {
        Shop shop = shopCache.getShopByOwner(user.getId());

        paymentService.upgradeShopPlan(shop, req.getTargetPlan(), req.getMonths());
        shopService.save(shop);

        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @GetMapping("/history")
    @Operation(summary = "Lịch sử nâng cấp gói", description = "Trả về lịch sử các lần nâng cấp gói dịch vụ của cửa hàng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy lịch sử thành công"),
            @ApiResponse(responseCode = "401", description = "Không xác thực hoặc token không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ApiResponseDto<List<SubscriptionHistory>> getHistory(
            @AuthenticationPrincipal @Parameter(description = "Thông tin người dùng hiện tại") CustomUserDetails user) {
        Shop shop = shopCache.getShopByOwner(user.getId());
        List<SubscriptionHistory> history = subscriptionHistoryRepository.findByShopIdOrderByCreatedAtDesc(shop.getId());
        return ApiResponseDto.success(ApiCode.SUCCESS, history);
    }
}
