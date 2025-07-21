// File: src/test/java/com/example/sales/service/UserServiceTest.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.User;
import com.example.sales.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @Test
    void testGetCurrentUser_shouldReturnSameUser() {
        User mockUser = new User();
        mockUser.setId("user1");

        User result = userService.getCurrentUser("user1");

        assertEquals("user1", result.getId());
    }

    @Test
    void testUpdateProfile_shouldUpdateAndSave() {
        User user = new User();
        user.setId("u1");

        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.updateProfile("u1", "Nguyễn Văn A", "0123456789", "Cửa hàng tiện lợi");

        assertEquals("Nguyễn Văn A", updated.getFullName());
        assertEquals("0123456789", updated.getPhone());
        assertEquals("Cửa hàng tiện lợi", updated.getBusinessType());

        verify(userRepository).save(user);
    }

    @Test
    void testChangePassword_shouldThrowExceptionIfCurrentPasswordWrong() {
        User user = new User();
        user.setPassword("encoded-old");

        when(passwordEncoder.matches("wrong-pass", "encoded-old")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.changePassword("user1", "wrong-pass", "new-pass")
        );

        assertEquals(ApiCode.INCORRECT_PASSWORD, ex.getError());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testChangePassword_shouldEncodeAndSaveNewPassword() {
        User user = new User();
        user.setPassword("encoded-old");

        when(passwordEncoder.matches("old-pass", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userService.changePassword("user1", "old-pass", "new-pass");

        assertEquals("encoded-new", user.getPassword());
        verify(userRepository).save(user);
    }
}
