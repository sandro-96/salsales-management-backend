// File: src/main/java/com/example/sales/controller/shop/ShopUserController.java
package com.example.sales.controller.shop;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.service.ShopUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop-users")
@RequiredArgsConstructor
@Validated
public class ShopUserController {

    private final ShopUserService shopUserService;

    @GetMapping("/my")
    @Operation(summary = "Lấy danh sách cửa hàng của người dùng hiện tại",
            description = "Trả về danh sách các cửa hàng mà người dùng hiện tại sở hữu hoặc tham gia")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy cửa hàng")
    })
    public ApiResponseDto<?> getMyShops(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(required = false) Pageable pageable
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS, shopUserService.getShopsForUser(customUserDetails.getId(), pageable));
    }
}
