package com.example.sales.controller.admin;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
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
 * Admin inbox hỗ trợ: cross-shop, chỉ truy cập bởi user có {@code ROLE_ADMIN}
 * (được gate ở {@code SecurityConfig} cho toàn bộ {@code /api/admin/**}).
 */
@RestController
@RequestMapping("/api/admin/support")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin — Support", description = "Inbox hỗ trợ xuyên suốt các shop (system admin)")
public class AdminSupportController {

    private final SupportTicketService ticketService;

    @GetMapping
    @Operation(summary = "Danh sách ticket toàn hệ thống (filter status/priority/shop/assignee/keyword)")
    public ApiResponseDto<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String shopId,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                ticketService.adminListTickets(status, priority, shopId, assigneeId, keyword,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/stats")
    @Operation(summary = "Đếm ticket theo trạng thái (admin dashboard)")
    public ApiResponseDto<?> stats() {
        return ApiResponseDto.success(ApiCode.SUCCESS, ticketService.adminStats());
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "Chi tiết ticket (admin)")
    public ApiResponseDto<?> detail(@PathVariable String ticketId) {
        return ApiResponseDto.success(ApiCode.SUCCESS, ticketService.adminGetTicket(ticketId));
    }

    @PostMapping("/{ticketId}/reply")
    @Operation(summary = "Admin phản hồi ticket")
    public ApiResponseDto<?> reply(
            @PathVariable String ticketId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ReplyTicketRequest request
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                ticketService.adminReply(ticketId, request, user.getId()));
    }

    @PutMapping("/{ticketId}/status")
    @Operation(summary = "Admin cập nhật trạng thái / assign ticket")
    public ApiResponseDto<?> updateStatus(
            @PathVariable String ticketId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody UpdateTicketStatusRequest request
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                ticketService.adminUpdateStatus(ticketId, request, user.getId()));
    }
}
