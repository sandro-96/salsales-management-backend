package com.example.sales.service;

import com.example.sales.constant.ApiErrorCode;
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

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ApiErrorCode.USER_NOT_FOUND));
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

    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }
}
