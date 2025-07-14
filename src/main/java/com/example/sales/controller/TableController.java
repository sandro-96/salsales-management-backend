// File: src/main/java/com/example/sales/controller/TableController.java

package com.example.sales.controller;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.TableStatus;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.TableRequest;
import com.example.sales.dto.TableResponse;
import com.example.sales.service.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/table")
@RequiredArgsConstructor
@Validated
public class TableController {

    private final TableService tableService;

    @PostMapping
    public ResponseEntity<ApiResponse<TableResponse>> create(
            @Valid @RequestBody TableRequest request
    ) {
        TableResponse response = tableService.create(request);
        return ResponseEntity.ok(ApiResponse.success(ApiCode.TABLE_CREATED, response));
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<ApiResponse<List<TableResponse>>> getByShop(
            @PathVariable String shopId,
            @RequestParam(required = false) String branchId
    ) {
        List<TableResponse> tables = tableService.getByShop(shopId, branchId);
        return ResponseEntity.ok(ApiResponse.success(ApiCode.TABLE_LIST, tables));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TableResponse>> updateStatus(
            @PathVariable String id,
            @RequestParam TableStatus status
    ) {
        TableResponse response = tableService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(ApiCode.TABLE_STATUS_UPDATED, response));
    }
}
