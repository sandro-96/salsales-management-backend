// File: src/test/java/com/example/sales/service/ShopServiceTest.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopType;
import com.example.sales.dto.shop.ShopRequest;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Shop;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.ShopUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopServiceTest {

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ShopUserRepository shopUserRepository;

    @InjectMocks
    private ShopService shopService;

    @Test
    void testCreateShop() {
        ShopRequest request = new ShopRequest();
        request.setName("Test Shop");
        request.setType(ShopType.RETAIL);
        request.setPhone("+123456789");
        request.setAddress("123 Street");

        when(shopRepository.findByOwnerIdAndDeletedFalse("user1")).thenReturn(Optional.empty());
        when(shopRepository.save(any(Shop.class))).thenAnswer(i -> i.getArgument(0));
        when(shopUserRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Shop shop = shopService.createShop("user1", request, "logo.png");

        assertEquals("Test Shop", shop.getName());
        assertEquals(ShopType.RETAIL, shop.getType());
        verify(auditLogService).log(eq("user1"), eq(shop.getId()), eq(shop.getId()), eq("SHOP"), eq("CREATED"), anyString());
    }

    @Test
    void testCreateShop_AlreadyExists() {
        ShopRequest request = new ShopRequest();
        request.setName("Test Shop");
        request.setType(ShopType.RETAIL);

        when(shopRepository.findByOwnerIdAndDeletedFalse("user1")).thenReturn(Optional.of(new Shop()));

        assertThrows(BusinessException.class, () -> shopService.createShop("user1", request, null),
                ApiCode.SHOP_ALREADY_EXISTS.getMessage());
    }
}