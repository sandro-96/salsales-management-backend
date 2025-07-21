// File: src/main/java/com/example/sales/controller/shop/ShopController.java
package com.example.sales.controller.shop;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.shop.ShopRequest;
import com.example.sales.dto.shop.ShopSimpleResponse;
import com.example.sales.model.Shop;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequireRole;
import com.example.sales.service.FileUploadService;
import com.example.sales.service.ShopService;
import com.example.sales.service.ShopUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
@Validated
public class ShopController {

    private final ShopService shopService;
    private final ShopUserService shopUserService;
    private final FileUploadService fileUploadService;

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Tạo cửa hàng mới", description = "Tạo cửa hàng với thông tin và logo tùy chọn")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cửa hàng được tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ApiResponseDto<Shop> create(@AuthenticationPrincipal CustomUserDetails user,
                                       @RequestPart("shop") @Valid ShopRequest request,
                                       @RequestPart(value = "file", required = false) MultipartFile file) {
        String logoUrl = null;
        if (file != null && !file.isEmpty()) {
            logoUrl = fileUploadService.upload(file);
        }
        return ApiResponseDto.success(ApiCode.SUCCESS, shopService.createShop(user.getId(), request, logoUrl));
    }

    @GetMapping("/me")
    @RequireRole(ShopRole.OWNER)
    @Operation(summary = "Lấy thông tin cửa hàng hiện tại", description = "Trả về thông tin chi tiết của cửa hàng mà người dùng hiện tại sở hữu")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trả về thông tin cửa hàng thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy cửa hàng")
    })
    public ApiResponseDto<?> getMyShop(@AuthenticationPrincipal CustomUserDetails user) {
        Shop shop = shopService.getShopByOwner(user.getId());
        return ApiResponseDto.success(ApiCode.SUCCESS, shopService.getShopResponse(user, shop));
    }

    @PutMapping("/me")
    @RequireRole(ShopRole.OWNER)
    @Operation(summary = "Cập nhật thông tin cửa hàng", description = "Cập nhật tên, địa chỉ, số điện thoại hoặc logo của cửa hàng")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền cập nhật"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy cửa hàng")
    })
    public ApiResponseDto<Shop> update(@AuthenticationPrincipal CustomUserDetails user,
                                       @RequestBody @Valid ShopRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS, shopService.updateShop(user.getId(), request));
    }

    @GetMapping("/my")
    @Operation(summary = "Lấy danh sách các cửa hàng mà người dùng tham gia", description = "Trả về danh sách shop mà người dùng hiện tại có vai trò trong đó (OWNER, STAFF,...)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trả về danh sách thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ApiResponseDto<Page<ShopSimpleResponse>> getMyShops(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "Thông tin phân trang (page, size, sort)") Pageable pageable
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS, shopUserService.getShopsForUser(user.getId(), pageable));
    }

    @DeleteMapping
    @RequireRole(ShopRole.OWNER)
    @Operation(summary = "Xóa cửa hàng hiện tại", description = "Xóa cửa hàng mà người dùng hiện tại đang sở hữu. Thao tác không thể hoàn tác.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xóa thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền xóa"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy cửa hàng")
    })
    public ApiResponseDto<?> deleteShop(@AuthenticationPrincipal CustomUserDetails user) {
        shopService.deleteShop(user.getId());
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    // File: ShopController.java
    @GetMapping("/search")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    @Operation(summary = "Tìm kiếm cửa hàng", description = "Tìm kiếm cửa hàng theo tên hoặc địa chỉ")
    public ApiResponseDto<Page<ShopSimpleResponse>> search(
            @RequestParam String keyword,
            Pageable pageable) {
        return ApiResponseDto.success(ApiCode.SUCCESS, shopService.searchShops(keyword, pageable));
    }
}

