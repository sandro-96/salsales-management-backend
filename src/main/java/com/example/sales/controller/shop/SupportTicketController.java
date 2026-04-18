package com.example.sales.controller.shop;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.support.CreateTicketRequest;
import com.example.sales.dto.support.ReplyTicketRequest;
import com.example.sales.dto.support.UpdateTicketStatusRequest;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.security.RequireRole;
import com.example.sales.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shops/{shopId}/support")
@RequiredArgsConstructor
@Validated
public class SupportTicketController {

    private final SupportTicketService ticketService;

    @PostMapping
    @Operation(summary = "Tạo ticket hỗ trợ mới")
    public ApiResponseDto<?> createTicket(
            @PathVariable String shopId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateTicketRequest request
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                ticketService.createTicket(shopId, request, user.getId()));
    }

    @GetMapping
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Danh sách tất cả ticket (quản lý)")
    public ApiResponseDto<?> getTickets(
            @PathVariable String shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                ticketService.getTickets(shopId, status, category, keyword,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/my")
    @Operation(summary = "Danh sách ticket của tôi")
    public ApiResponseDto<?> getMyTickets(
            @PathVariable String shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                ticketService.getMyTickets(shopId, user.getId(),
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "Chi tiết ticket")
    public ApiResponseDto<?> getTicket(
            @PathVariable String shopId,
            @PathVariable String ticketId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                ticketService.getTicket(shopId, ticketId));
    }

    @PostMapping("/{ticketId}/reply")
    @Operation(summary = "Phản hồi ticket")
    public ApiResponseDto<?> replyToTicket(
            @PathVariable String shopId,
            @PathVariable String ticketId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ReplyTicketRequest request
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                ticketService.replyToTicket(shopId, ticketId, request, user.getId()));
    }

    @PutMapping("/{ticketId}/status")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Cập nhật trạng thái ticket")
    public ApiResponseDto<?> updateTicketStatus(
            @PathVariable String shopId,
            @PathVariable String ticketId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody UpdateTicketStatusRequest request
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                ticketService.updateTicketStatus(shopId, ticketId, request, user.getId()));
    }

    @DeleteMapping("/{ticketId}")
    @RequireRole({ShopRole.OWNER, ShopRole.MANAGER})
    @Operation(summary = "Xoá ticket (soft delete)")
    public ApiResponseDto<?> deleteTicket(
            @PathVariable String shopId,
            @PathVariable String ticketId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        ticketService.deleteTicket(shopId, ticketId, user.getId());
        return ApiResponseDto.success(ApiCode.SUCCESS, null);
    }
}
