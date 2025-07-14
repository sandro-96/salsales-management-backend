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

    public User getCurrentUser(User user) {
        return user;
    }

    public User updateProfile(User user, String fullName, String phone, String businessType) {
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setBusinessType(businessType);
        return userRepository.save(user);
    }

    public void changePassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(ApiCode.INCORRECT_PASSWORD);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
