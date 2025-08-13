package com.example.sales.dto;

import com.example.sales.constant.Country;

public record CountryOption(String value, String code, String name, String dialCode, String phonePattern) {
    public static CountryOption from(Country type) {
        return new CountryOption(type.name(), type.getCode(), type.getName(), type.getDialCode(), type.getPhonePattern());
    }
}
