// File: src/main/java/com/example/sales/controller/shop/ShopUserController.java
package com.example.sales.controller.shop;

import com.example.sales.service.ShopUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop-users")
@RequiredArgsConstructor
@Validated
public class ShopUserController {

    private final ShopUserService shopUserService;
}
