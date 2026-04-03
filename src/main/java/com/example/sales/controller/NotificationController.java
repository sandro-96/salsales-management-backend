package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.security.CustomUserDetails;
import com.example.sales.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Danh sách thông báo của tôi")
    public ApiResponseDto<?> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String shopId,
            @RequestParam(required = false) String read,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                notificationService.getNotifications(user.getId(), shopId, read,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Số thông báo chưa đọc")
    public ApiResponseDto<?> getUnreadCount(
            @RequestParam(required = false) String shopId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        long count = notificationService.getUnreadCount(user.getId(), shopId);
        return ApiResponseDto.success(ApiCode.SUCCESS, Map.of("count", count));
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Đánh dấu đã đọc 1 thông báo")
    public ApiResponseDto<?> markAsRead(
            @PathVariable String notificationId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                notificationService.markAsRead(notificationId, user.getId()));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Đánh dấu đã đọc tất cả thông báo")
    public ApiResponseDto<?> markAllAsRead(
            @RequestParam(required = false) String shopId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        notificationService.markAllAsRead(user.getId(), shopId);
        return ApiResponseDto.success(ApiCode.SUCCESS, null);
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Xoá thông báo")
    public ApiResponseDto<?> deleteNotification(
            @PathVariable String notificationId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        notificationService.deleteNotification(notificationId, user.getId());
        return ApiResponseDto.success(ApiCode.SUCCESS, null);
    }
}
