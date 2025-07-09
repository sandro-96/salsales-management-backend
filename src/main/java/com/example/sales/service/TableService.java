package com.example.sales.service;

import com.example.sales.constant.ApiErrorCode;
import com.example.sales.constant.ApiMessage;
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
import java.util.stream.Collectors;

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
                .status(request.getStatus() != null ? request.getStatus() : TableStatus.AVAILABLE)
                .shop(shop)
                .build();

        tableRepository.save(table);
        return toResponse(table);
    }

    public List<TableResponse> getByShop(String shopId) {
        return tableRepository.findByShopId(shopId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public TableResponse updateStatus(String tableId, TableStatus status) {
        Table table = tableRepository.findById(tableId)
                .orElseThrow(() -> new BusinessException(ApiErrorCode.TABLE_NOT_FOUND));

        table.setStatus(status);
        tableRepository.save(table);
        return toResponse(table);
    }

    private TableResponse toResponse(Table table) {
        return TableResponse.builder()
                .id(table.getId())
                .name(table.getName())
                .status(table.getStatus())
                .shopId(table.getShop().getId())
                .shopName(table.getShop().getName())
                .build();
    }
}
