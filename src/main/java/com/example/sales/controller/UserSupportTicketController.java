package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.support.CreateTicketRequest;
import com.example.sales.dto.support.ReplyTicketRequest;
import com.example.sales.dto.support.UpdateTicketStatusRequest;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Hỗ trợ giữa user đăng nhập và admin hệ thống — không gắn shop.
 */
@RestController
@RequestMapping("/api/user/support")
@RequiredArgsConstructor
@Validated
@Tag(name = "User — Support", description = "Ticket hỗ trợ theo tài khoản user")
public class UserSupportTicketController {

    private final SupportTicketService ticketService;

    @PostMapping
    @Operation(summary = "Tạo ticket hỗ trợ")
    public ApiResponseDto<?> createTicket(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateTicketRequest request
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                ticketService.createUserTicket(request, user.getId()));
    }

    @GetMapping
    @Operation(summary = "Danh sách ticket của tôi")
    public ApiResponseDto<?> listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                ticketService.listUserTickets(user.getId(), status, category, keyword,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "Chi tiết ticket (của tôi)")
    public ApiResponseDto<?> getTicket(
            @PathVariable String ticketId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                ticketService.getUserTicket(user.getId(), ticketId));
    }

    @PostMapping("/{ticketId}/reply")
    @Operation(summary = "Phản hồi ticket")
    public ApiResponseDto<?> reply(
            @PathVariable String ticketId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ReplyTicketRequest request
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                ticketService.replyUserTicket(ticketId, request, user.getId()));
    }

    @PutMapping("/{ticketId}/status")
    @Operation(summary = "Cập nhật trạng thái ticket (chủ ticket)")
    public ApiResponseDto<?> updateStatus(
            @PathVariable String ticketId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody UpdateTicketStatusRequest request
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                ticketService.updateUserTicketStatus(ticketId, request, user.getId()));
    }

    @DeleteMapping("/{ticketId}")
    @Operation(summary = "Xoá ticket (chủ ticket)")
    public ApiResponseDto<?> deleteTicket(
            @PathVariable String ticketId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        ticketService.deleteUserTicket(ticketId, user.getId());
        return ApiResponseDto.success(ApiCode.SUCCESS, null);
    }
}
