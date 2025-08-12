package com.example.sales.constant;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum Country {
    VN("VN", "Việt Nam", "+84", "^0(3|5|7|8|9)\\d{8}$"),
    US("US", "Hoa Kỳ", "+1", "^\\d{10}$"),
    JP("JP", "Nhật Bản", "+81", "^\\d{10}$"),
    KR("KR", "Hàn Quốc", "+82", "^\\d{9,10}$");

    private final String code;
    private final String name;
    private final String dialCode;
    private final String phonePattern;

    Country(String code, String name, String dialCode, String phonePattern) {
        this.code = code;
        this.name = name;
        this.dialCode = dialCode;
        this.phonePattern = phonePattern;
    }

    private static final Map<String, Country> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toMap(Country::getCode, Function.identity()));

    public static Country fromCode(String code) {
        Country country = BY_CODE.get(code);
        if (country == null) {
            throw new IllegalArgumentException("Mã quốc gia không hợp lệ: " + code);
        }
        return country;
    }
}