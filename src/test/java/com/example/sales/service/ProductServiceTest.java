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
        req.setPrice(10000.0);
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

    @Test
    void testUpdateProduct() {
        Product product = Product.builder()
                .id("p1")
                .shopId("shop1")
                .name("Cà phê")
                .price(10000.0)
                .quantity(10)
                .category("Đồ uống")
                .build();
        when(productRepository.findByIdAndShopIdAndDeletedFalse("p1", "shop1"))
                .thenReturn(Optional.of(product));
        when(shopRepository.findByIdAndDeletedFalse("shop1"))
                .thenReturn(Optional.of(Shop.builder().id("shop1").type(ShopType.CAFE).build()));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProductRequest req = new ProductRequest();
        req.setName("Trà sữa");
        req.setPrice(15000.0);
        req.setQuantity(20);
        req.setCategory("Đồ uống");

        ProductResponse response = productService.updateProduct("user1", "shop1", "p1", req);

        assertEquals("Trà sữa", response.getName());
        assertEquals(15000.0, response.getPrice());
        verify(auditLogService).log(eq("user1"), eq("shop1"), eq("p1"), eq("PRODUCT"), eq("PRICE_CHANGED"), anyString());
    }
}
