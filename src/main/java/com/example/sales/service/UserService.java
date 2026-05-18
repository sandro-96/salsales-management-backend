// File: src/main/java/com/example/sales/service/UserService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.Country;
import com.example.sales.dto.UpdateProfileRequest;
import com.example.sales.dto.user.UserResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.User;
import com.example.sales.repository.UserRepository;
import com.example.sales.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));
        return buildUserResponse(user);
    }

    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));

        // Validate only when both country and non-empty phone are sent (partial PATCH-style updates).
        if (StringUtils.hasText(request.getCountryCode()) && StringUtils.hasText(request.getPhone())) {
            Country country = Country.fromCode(request.getCountryCode().trim());
            String phone = request.getPhone().trim();
            if (!phone.matches(country.getPhonePattern())) {
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
            if (StringUtils.hasText(request.getPhone())) {
                String phoneCompact = PhoneUtils.compact(request.getPhone());
                user.setPhone(phoneCompact);
                user.setPhoneNormalized(PhoneUtils.normalizeForMatch(phoneCompact));
            } else {
                user.setPhone(null);
                user.setPhoneNormalized(null);
            }
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
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }
        if (request.getLanguage() != null && !request.getLanguage().isBlank()) {
            user.setLanguage(request.getLanguage().trim().toLowerCase());
        }
        return buildUserResponse(userRepository.save(user));
    }

    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));
        boolean hasLocalPassword = StringUtils.hasText(user.getPassword());
        if (hasLocalPassword) {
            if (!StringUtils.hasText(currentPassword)) {
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new BusinessException(ApiCode.INCORRECT_PASSWORD);
            }
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
                .gender(user.getGender())
                .passwordSet(StringUtils.hasText(user.getPassword()))
                .build();
    }
}
