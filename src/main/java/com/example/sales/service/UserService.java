// File: src/main/java/com/example/sales/service/UserService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.Country;
import com.example.sales.dto.UpdateProfileRequest;
import com.example.sales.dto.user.UserResponse;
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

    public UserResponse getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .middleName(user.getMiddleName())
                .birthDate(user.getBirthDate())
                .timezone(user.getTimezone())
                .currency(user.getCurrency())
                .language(user.getLanguage())
                .phone(user.getPhone())
                .city(user.getCity())
                .state(user.getState())
                .countryCode(user.getCountryCode())
                .zipCode(user.getZipCode())
                .address(user.getAddress())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .verified(user.isVerified())
                .active(user.isActive())
                .lastLoginAt(user.getLastLoginAt())
                .gender(user.getGender())
                .build();
    }

    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));

        // Validate phone number if countryCode is provided
        if (request.getCountryCode() != null && request.getPhone() != null) {
            Country country = Country.fromCode(request.getCountryCode());
            if (!request.getPhone().matches(country.getPhonePattern())) {
                throw new BusinessException(ApiCode.INVALID_PHONE_NUMBER);
            }
        }

        // Update fields if provided (not null)
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getMiddleName() != null) {
            user.setMiddleName(request.getMiddleName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getState() != null) {
            user.setState(request.getState());
        }
        if (request.getZipCode() != null) {
            user.setZipCode(request.getZipCode());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getCountryCode() != null) {
            Country.fromCode(request.getCountryCode()); // Validate country code
            user.setCountryCode(request.getCountryCode());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        return buildUserResponse(userRepository.save(user));
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

    private UserResponse buildUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .middleName(user.getMiddleName())
                .birthDate(user.getBirthDate())
                .timezone(user.getTimezone())
                .currency(user.getCurrency())
                .language(user.getLanguage())
                .phone(user.getPhone())
                .city(user.getCity())
                .state(user.getState())
                .countryCode(user.getCountryCode())
                .zipCode(user.getZipCode())
                .address(user.getAddress())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .verified(user.isVerified())
                .active(user.isActive())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
