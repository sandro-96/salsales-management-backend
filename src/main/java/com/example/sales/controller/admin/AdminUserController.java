// File: src/main/java/com/example/sales/controller/admin/AdminUserController.java

package com.example.sales.controller.admin;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.User;
import com.example.sales.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Validated
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ApiResponseDto<List<User>> getAll() {
        return ApiResponseDto.success(ApiCode.SUCCESS, adminUserService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ApiResponseDto<User> getById(@PathVariable String id) {
        return ApiResponseDto.success(ApiCode.SUCCESS, adminUserService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ApiResponseDto<User> updateUser(@PathVariable String id,
                                           @RequestBody User userUpdate) {
        return ApiResponseDto.success(ApiCode.USER_UPDATED, adminUserService.updateUser(id, userUpdate));
    }

    @DeleteMapping("/{id}")
    public ApiResponseDto<?> deleteUser(@PathVariable String id,
                                        @AuthenticationPrincipal User currentUser) {
        if (id.equals(currentUser.getId())) {
            throw new BusinessException(ApiCode.CANNOT_DELETE_SELF);
        }

        adminUserService.deleteUser(id);
        return ApiResponseDto.success(ApiCode.USER_DELETED);
    }
}
