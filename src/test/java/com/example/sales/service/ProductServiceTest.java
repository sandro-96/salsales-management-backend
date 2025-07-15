// File: test/java/com/example/sales/service/ProductServiceTest.java
package com.example.sales.service;

import com.example.sales.constant.ShopType;
import com.example.sales.dto.product.ProductRequest;
import com.example.sales.dto.product.ProductResponse;
import com.example.sales.model.Product;
import com.example.sales.model.Shop;
import com.example.sales.repository.ProductRepository;
import com.example.sales.repository.ShopRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock private ProductRepository productRepository;
    @Mock private ShopRepository shopRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private FileUploadService fileUploadService;

    @Test
    void testCreateProduct() {
        // Arrange
        ProductRequest req = new ProductRequest();
        req.setName("Cà phê");
        req.setPrice(25000);
        req.setQuantity(10);
        req.setCategory("Đồ uống");
        req.setUnit("Cốc");
        req.setImageUrl("http://example.com/image.jpg");

        Shop mockShop = Shop.builder()
                .id("shop1")
                .name("Quán A")
                .type(ShopType.CAFE)
                .build();

        when(shopRepository.findByIdAndDeletedFalse("shop1"))
                .thenReturn(Optional.of(mockShop));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product product = invocation.getArgument(0);
                    product.setId("p1");
                    return product;
                });

        // Act
        ProductResponse response = productService.createProduct("shop1", req);

        // Assert
        assertEquals("Cà phê", response.getName());
        assertEquals("shop1", response.getShopId());

        verify(productRepository).save(any(Product.class));
        verify(fileUploadService).moveToProduct(eq("http://example.com/image.jpg"));
        verify(auditLogService).log(
                eq(null),
                eq("shop1"),
                eq("p1"),
                eq("PRODUCT"),
                eq("CREATED"),
                contains("Cà phê")
        );
    }
}
