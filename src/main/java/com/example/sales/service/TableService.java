package com.example.sales.service;

import com.example.sales.constant.ApiErrorCode;
import com.example.sales.constant.TableStatus;
import com.example.sales.dto.TableRequest;
import com.example.sales.dto.TableResponse;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Shop;
import com.example.sales.model.Table;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TableService {

    private final TableRepository tableRepository;
    private final ShopRepository shopRepository;

    public TableResponse create(TableRequest request) {
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new BusinessException(ApiErrorCode.SHOP_NOT_FOUND));

        Table table = Table.builder()
                .name(request.getName())
                .shopId(shop.getId())
                .status(Optional.ofNullable(request.getStatus()).orElse(TableStatus.AVAILABLE))
                .capacity(request.getCapacity())
                .note(request.getNote())
                .build();

        return toResponse(tableRepository.save(table), shop);
    }

    public List<TableResponse> getByShop(String shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new BusinessException(ApiErrorCode.SHOP_NOT_FOUND));

        return tableRepository.findByShopId(shopId).stream()
                .map(table -> toResponse(table, shop))
                .toList();
    }

    public TableResponse updateStatus(String tableId, TableStatus status) {
        Table table = tableRepository.findById(tableId)
                .orElseThrow(() -> new BusinessException(ApiErrorCode.TABLE_NOT_FOUND));

        table.setStatus(status);
        return toResponse(tableRepository.save(table));
    }

    private TableResponse toResponse(Table table) {
        Shop shop = shopRepository.findById(table.getShopId()).orElse(null);

        return TableResponse.builder()
                .id(table.getId())
                .name(table.getName())
                .status(table.getStatus())
                .shopId(table.getShopId())
                .shopName(shop != null ? shop.getName() : null)
                .capacity(table.getCapacity())
                .note(table.getNote())
                .currentOrderId(table.getCurrentOrderId())
                .build();
    }

    private TableResponse toResponse(Table table, Shop shop) {
        return TableResponse.builder()
                .id(table.getId())
                .name(table.getName())
                .status(table.getStatus())
                .shopId(shop.getId())
                .shopName(shop.getName())
                .capacity(table.getCapacity())
                .note(table.getNote())
                .currentOrderId(table.getCurrentOrderId())
                .build();
    }
}

