package com.example.sales.controller;

import com.example.sales.constant.ApiMessage;
import com.example.sales.constant.TableStatus;
import com.example.sales.dto.ApiResponse;
import com.example.sales.dto.TableRequest;
import com.example.sales.dto.TableResponse;
import com.example.sales.service.TableService;
import com.example.sales.util.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/table")
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;
    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<ApiResponse<TableResponse>> create(
            @Valid @RequestBody TableRequest request,
            Locale locale
    ) {
        TableResponse response = tableService.create(request);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.TABLE_CREATED, response, messageService, locale));
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<ApiResponse<List<TableResponse>>> getByShop(
            @PathVariable String shopId,
            @RequestParam(required = false) String branchId,
            Locale locale
    ) {
        List<TableResponse> tables = tableService.getByShop(shopId, branchId);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.TABLE_LIST, tables, messageService, locale));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TableResponse>> updateStatus(
            @PathVariable String id,
            @RequestParam TableStatus status,
            Locale locale
    ) {
        TableResponse response = tableService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(ApiMessage.TABLE_STATUS_UPDATED, response, messageService, locale));
    }
}
