// File: src/main/java/com/example/sales/service/ShopService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.dto.ShopRequest;
import com.example.sales.exception.BusinessException;
import com.example.sales.model.Shop;
import com.example.sales.model.User;
import com.example.sales.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final AuditLogService auditLogService;

    public Shop createShop(User owner, ShopRequest request) {
        if (shopRepository.findByOwnerIdAndDeletedFalse(owner.getId()).isPresent()) {
            throw new BusinessException(ApiCode.SHOP_ALREADY_EXISTS);
        }

        Shop shop = new Shop();
        shop.setName(request.getName());
        shop.setType(request.getType());
        shop.setAddress(request.getAddress());
        shop.setPhone(request.getPhone());
        shop.setLogoUrl(request.getLogoUrl());
        shop.setOwnerId(owner.getId());

        Shop saved = shopRepository.save(shop);
        auditLogService.log(owner, saved.getId(), saved.getId(), "SHOP", "CREATED",
                String.format("Tạo cửa hàng: %s (%s)", saved.getName(), saved.getType()));
        return saved;
    }

    public Shop getShopByOwner(String ownerId) {
        return shopRepository.findByOwnerIdAndDeletedFalse(ownerId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND));
    }

    public Shop updateShop(String ownerId, ShopRequest request) {
        Shop shop = getShopByOwner(ownerId);

        shop.setName(request.getName());
        shop.setType(request.getType());
        shop.setAddress(request.getAddress());
        shop.setPhone(request.getPhone());
        shop.setLogoUrl(request.getLogoUrl());

        Shop saved = shopRepository.save(shop);
        auditLogService.log(null, saved.getId(), saved.getId(), "SHOP", "UPDATED",
                String.format("Cập nhật cửa hàng: %s (%s)", saved.getName(), saved.getType()));
        return saved;
    }

    public String getShopIdByOwner(String ownerId) {
        return shopRepository.findByOwnerIdAndDeletedFalse(ownerId)
                .orElseThrow(() -> new BusinessException(ApiCode.SHOP_NOT_FOUND))
                .getId();
    }

    public void deleteShop(String ownerId) {
        Shop shop = getShopByOwner(ownerId);
        shop.setDeleted(true);
        shopRepository.save(shop);
        auditLogService.log(null, shop.getId(), shop.getId(), "SHOP", "DELETED",
                String.format("Xoá mềm cửa hàng: %s", shop.getName()));
    }

}
