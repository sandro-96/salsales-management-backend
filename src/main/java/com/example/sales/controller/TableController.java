// File: src/main/java/com/example/sales/controller/TableController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.ShopRole;
import com.example.sales.constant.TableStatus;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.TableRequest;
import com.example.sales.dto.TableResponse;
import com.example.sales.security.RequireRole;
import com.example.sales.service.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
@Validated
public class TableController {

    private final TableService tableService;

    @PostMapping
    @RequireRole(ShopRole.OWNER)
    public ApiResponse<TableResponse> create(@RequestBody @Valid TableRequest request) {
        return ApiResponse.success(ApiCode.SUCCESS, tableService.create(request));
    }

    @GetMapping
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<List<TableResponse>> getByShop(@RequestParam String shopId,
                                                      @RequestParam String branchId) {
        return ApiResponse.success(ApiCode.SUCCESS, tableService.getByShop(shopId, branchId));
    }

    @PutMapping("/{id}/status")
    @RequireRole({ShopRole.OWNER, ShopRole.STAFF})
    public ApiResponse<TableResponse> updateStatus(@PathVariable String id,
                                                   @RequestParam TableStatus status) {
        return ApiResponse.success(ApiCode.SUCCESS, tableService.updateStatus(id, status));
    }
}
