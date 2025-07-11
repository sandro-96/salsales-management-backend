// File: src/main/java/com/example/sales/controller/admin/AdminUserController.java

package com.example.sales.controller.admin;

import com.example.sales.constant.ApiErrorCode;
import com.example.sales.constant.ApiMessage;
import com.example.sales.dto.ApiResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.User;
import com.example.sales.service.AdminUserService;
import com.example.sales.util.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Validated
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final MessageService messageService;

    @GetMapping
    public ApiResponse<List<User>> getAll(Locale locale) {
        return ApiResponse.success(ApiMessage.USER_LIST, adminUserService.getAllUsers(), messageService, locale);
    }

    @GetMapping("/{id}")
    public ApiResponse<User> getById(@PathVariable String id, Locale locale) {
        return ApiResponse.success(ApiMessage.USER_DETAIL, adminUserService.getUserById(id), messageService, locale);
    }

    @PutMapping("/{id}")
    public ApiResponse<User> updateUser(@PathVariable String id,
                                        @RequestBody User userUpdate,
                                        Locale locale) {
        return ApiResponse.success(ApiMessage.USER_UPDATED, adminUserService.updateUser(id, userUpdate), messageService, locale);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteUser(@PathVariable String id,
                                     @AuthenticationPrincipal User currentUser,
                                     Locale locale) {
        if (id.equals(currentUser.getId())) {
            throw new BusinessException(ApiErrorCode.CANNOT_DELETE_SELF);
        }

        adminUserService.deleteUser(id);
        return ApiResponse.success(ApiMessage.USER_DELETED, messageService, locale);
    }

}
