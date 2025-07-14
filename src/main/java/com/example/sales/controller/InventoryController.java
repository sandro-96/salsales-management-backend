// File: com/example/sales/controller/InventoryController.java
package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.inventory.InventoryRequest;
import com.example.sales.model.InventoryTransaction;
import com.example.sales.security.RequireRole;
import com.example.sales.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @RequireRole(ShopRole.OWNER)
    public ApiResponse<InventoryTransaction> create(@RequestParam String shopId,
                                                    @RequestBody @Valid InventoryRequest request) {
        return ApiResponse.success(ApiCode.SUCCESS, inventoryService.createTransaction(shopId, request));
    }

    @GetMapping("/history")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<List<InventoryTransaction>> getHistory(@RequestParam String productId) {
        return ApiResponse.success(ApiCode.SUCCESS, inventoryService.getHistory(productId));
    }
}
