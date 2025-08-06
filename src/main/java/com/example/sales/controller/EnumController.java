package com.example.sales.controller;

import com.example.sales.constant.*;
import com.example.sales.dto.ApiResponseDto;
import com.example.sales.dto.ShopTypeOption;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enums")
@RequiredArgsConstructor
public class EnumController {

    @GetMapping("/shop/all")
    public ApiResponseDto<Map<String, Object>> getAllShopEnums() {
        List<ShopTypeOption> shopTypes = Arrays.stream(ShopType.values())
                .map(ShopTypeOption::from)
                .toList();

        List<Map<String, String>>businessModels = Arrays.stream(BusinessModel.values())
                .map(bm -> Map.of(
                        "value", bm.name(),
                        "label", bm.getLabel()
                ))
                .toList();

        return ApiResponseDto.success(ApiCode.SUCCESS, Map.of(
                "shopTypes", shopTypes,
                "businessModels", businessModels
        ));
    }
}
