// File: src/main/java/com/example/sales/service/UserService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.User;
import com.example.sales.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User getCurrentUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));
    }

    public User updateProfile(String userId, String fullName, String phone, String businessType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setBusinessType(businessType);
        return userRepository.save(user);
    }

    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(ApiCode.INCORRECT_PASSWORD);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
