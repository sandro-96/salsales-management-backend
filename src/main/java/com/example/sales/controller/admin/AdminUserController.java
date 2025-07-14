// File: src/main/java/com/example/sales/controller/admin/AdminUserController.java

package com.example.sales.controller.admin;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ApiResponse;
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
    public ApiResponse<List<User>> getAll() {
        return ApiResponse.success(ApiCode.SUCCESS, adminUserService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ApiResponse<User> getById(@PathVariable String id) {
        return ApiResponse.success(ApiCode.SUCCESS, adminUserService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<User> updateUser(@PathVariable String id,
                                        @RequestBody User userUpdate) {
        return ApiResponse.success(ApiCode.USER_UPDATED, adminUserService.updateUser(id, userUpdate));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteUser(@PathVariable String id,
                                     @AuthenticationPrincipal User currentUser) {
        if (id.equals(currentUser.getId())) {
            throw new BusinessException(ApiCode.CANNOT_DELETE_SELF);
        }

        adminUserService.deleteUser(id);
        return ApiResponse.success(ApiCode.USER_DELETED);
    }
}
