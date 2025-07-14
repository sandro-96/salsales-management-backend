// File: src/main/java/com/example/sales/service/AdminUserService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.User;
import com.example.sales.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(String id) {
        return userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.USER_NOT_FOUND));
    }

    public User updateUser(String id, User update) {
        User user = getUserById(id);
        user.setEmail(update.getEmail());
        user.setFullName(update.getFullName());
        user.setPhone(update.getPhone());
        user.setBusinessType(update.getBusinessType());
        user.setVerified(update.isVerified());
        user.setRole(update.getRole());
        return userRepository.save(user);
    }

    public void deleteUser(String userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.USER_NOT_FOUND));
        user.setDeleted(true);
        userRepository.save(user);
        auditLogService.log(null, user.getId(), user.getId(), "USER", "DELETED", "Xoá mềm tài khoản người dùng");
    }
}
