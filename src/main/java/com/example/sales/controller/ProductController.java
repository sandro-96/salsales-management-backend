package com.example.sales.controller;

import com.example.sales.constant.ApiMessage;
import com.example.sales.dto.ApiResponse;
import com.example.sales.model.Product;
import com.example.sales.model.User;
import com.example.sales.service.ProductService;
import com.example.sales.util.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final MessageService messageService;

    @GetMapping
    public ApiResponse<List<Product>> getAllByUser(@AuthenticationPrincipal User user, Locale locale) {
        List<Product> products = productService.getAllByUser(user);
        return ApiResponse.success(ApiMessage.PRODUCT_LIST, products, messageService, locale);
    }

    @PostMapping
    public ApiResponse<Product> createProduct(@AuthenticationPrincipal User user,
                                              @RequestBody Product product,
                                              Locale locale) {
        Product created = productService.createProduct(user, product);
        return ApiResponse.success(ApiMessage.PRODUCT_CREATED, created, messageService, locale);
    }

    @PutMapping("/{id}")
    public ApiResponse<Product> updateProduct(@AuthenticationPrincipal User user,
                                              @PathVariable String id,
                                              @RequestBody Product product,
                                              Locale locale) {
        Product updated = productService.updateProduct(user, id, product);
        return ApiResponse.success(ApiMessage.PRODUCT_UPDATED, updated, messageService, locale);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteProduct(@AuthenticationPrincipal User user,
                                        @PathVariable String id,
                                        Locale locale) {
        productService.deleteProduct(user, id);
        return ApiResponse.success(ApiMessage.PRODUCT_DELETED, messageService, locale);
    }
}
