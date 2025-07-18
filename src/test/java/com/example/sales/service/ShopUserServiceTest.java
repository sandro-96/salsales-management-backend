// File: src/test/java/com/example/sales/service/ShopUserServiceTest.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.constant.ShopType;
import com.example.sales.dto.shop.ShopSimpleResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Shop;
import com.example.sales.model.ShopUser;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.ShopUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopUserServiceTest {

    @Mock
    private ShopUserRepository shopUserRepository;

    @Mock
    private ShopRepository shopRepository;

    @InjectMocks
    private ShopUserService shopUserService;

    @Test
    void testAddUser() {
        Shop shop = Shop.builder()
                .id("shop1")
                .name("Test Shop")
                .type(ShopType.RETAIL)
                .active(true)
                .build();
        when(shopRepository.findByIdAndDeletedFalse("shop1")).thenReturn(Optional.of(shop));
        when(shopUserRepository.findByShopIdAndUserIdAndDeletedFalse("shop1", "user1"))
                .thenReturn(Optional.empty());
        when(shopUserRepository.save(any(ShopUser.class))).thenAnswer(i -> i.getArgument(0));

        shopUserService.addUser("shop1", "user1", ShopRole.STAFF, null);

        verify(shopUserRepository).save(argThat(su ->
                su.getShopId().equals("shop1") &&
                        su.getUserId().equals("user1") &&
                        su.getRole() == ShopRole.STAFF));
    }

    @Test
    void testAddUser_Duplicate() {
        Shop shop = Shop.builder()
                .id("shop1")
                .name("Test Shop")
                .active(true)
                .build();
        ShopUser shopUser = ShopUser.builder()
                .shopId("shop1")
                .userId("user1")
                .role(ShopRole.STAFF)
                .build();
        when(shopRepository.findByIdAndDeletedFalse("shop1")).thenReturn(Optional.of(shop));
        when(shopUserRepository.findByShopIdAndUserIdAndDeletedFalse("shop1", "user1"))
                .thenReturn(Optional.of(shopUser));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> shopUserService.addUser("shop1", "user1", ShopRole.STAFF, null));
        assertEquals(ApiCode.DUPLICATE_DATA.getMessage(), exception.getMessage());
    }

    @Test
    void testAddUser_ShopNotFound() {
        when(shopRepository.findByIdAndDeletedFalse("shop1")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> shopUserService.addUser("shop1", "user1", ShopRole.STAFF, null));
        assertEquals(ApiCode.SHOP_NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    void testRemoveUser() {
        ShopUser shopUser = ShopUser.builder()
                .shopId("shop1")
                .userId("user1")
                .role(ShopRole.STAFF)
                .build();
        when(shopUserRepository.findByShopIdAndUserIdAndDeletedFalse("shop1", "user1"))
                .thenReturn(Optional.of(shopUser));

        shopUserService.removeUser("shop1", "user1");

        verify(shopUserRepository).delete(shopUser);
    }

    @Test
    void testRemoveUser_NotFound() {
        when(shopUserRepository.findByShopIdAndUserIdAndDeletedFalse("shop1", "user1"))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> shopUserService.removeUser("shop1", "user1"));
        assertEquals(ApiCode.NOT_FOUND.getMessage(), exception.getMessage());
    }

    @Test
    void testGetShopsForUser() {
        ShopUser shopUser = ShopUser.builder()
                .shopId("shop1")
                .userId("user1")
                .role(ShopRole.STAFF)
                .build();
        Shop shop = Shop.builder()
                .id("shop1")
                .name("Test Shop")
                .type(ShopType.RETAIL)
                .active(true)
                .build();
        Page<ShopUser> shopUserPage = new PageImpl<>(List.of(shopUser), PageRequest.of(0, 10), 1);
        when(shopUserRepository.findByUserIdAndDeletedFalse(eq("user1"), any(Pageable.class)))
                .thenReturn(shopUserPage);
        when(shopRepository.findByIdAndDeletedFalse("shop1")).thenReturn(Optional.of(shop));

        List<ShopSimpleResponse> shops = shopUserService.getShopsForUser("user1", PageRequest.of(0, 10)).getContent();

        assertEquals(1, shops.size());
        assertEquals("Test Shop", shops.get(0).getName());
        assertEquals(ShopRole.STAFF, shops.get(0).getRole());
        assertEquals(ShopType.RETAIL, shops.get(0).getType());
    }

    @Test
    void testGetShopsForUser_Empty() {
        Page<ShopUser> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(shopUserRepository.findByUserIdAndDeletedFalse(eq("user1"), any(Pageable.class)))
                .thenReturn(emptyPage);

        List<ShopSimpleResponse> shops = shopUserService.getShopsForUser("user1", PageRequest.of(0, 10)).getContent();

        assertTrue(shops.isEmpty());
    }

    @Test
    void testGetUserRoleInShop() {
        ShopUser shopUser = ShopUser.builder()
                .shopId("shop1")
                .userId("user1")
                .role(ShopRole.OWNER)
                .build();
        when(shopUserRepository.findByShopIdAndUserIdAndDeletedFalse("shop1", "user1"))
                .thenReturn(Optional.of(shopUser));

        ShopRole role = shopUserService.getUserRoleInShop("shop1", "user1");

        assertEquals(ShopRole.OWNER, role);
    }

    @Test
    void testGetUserRoleInShop_NotFound() {
        when(shopUserRepository.findByShopIdAndUserIdAndDeletedFalse("shop1", "user1"))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> shopUserService.getUserRoleInShop("shop1", "user1"));
        assertEquals(ApiCode.UNAUTHORIZED.getMessage(), exception.getMessage());
    }
}