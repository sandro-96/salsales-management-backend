// File: test/java/com/example/sales/service/ShopServiceTest.java
package com.example.sales.service;

import com.example.sales.dto.shop.ShopRequest;
import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.repository.ShopRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopServiceTest {

    @InjectMocks
    private ShopService shopService;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private AuditLogService auditLogService; // ✅ Thêm dòng này

    @Test
    void testCreateShop() {
        ShopRequest request = new ShopRequest();
        request.setName("Shop ABC");

        User owner = new User();
        owner.setId("u1");

        when(shopRepository.save(any())).thenAnswer(invocation -> {
            Shop shop = invocation.getArgument(0);
            shop.setId("s1");
            return shop;
        });

        Shop result = shopService.createShop("u1", request, null);

        assertEquals("Shop ABC", result.getName());
        verify(shopRepository).save(any(Shop.class));

        // ✅ Optional: kiểm tra ghi log
        verify(auditLogService).log(eq("u1"), eq("s1"), any(), eq("SHOP"), eq("CREATED"), contains("Shop ABC"));
    }
}


