package com.example.sales.dto;

import com.example.sales.constant.Gender;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 50, message = "Họ không được vượt quá 50 ký tự")
    private String lastName;

    @Size(max = 50, message = "Tên không được vượt quá 50 ký tự")
    private String firstName;

    @Size(max = 50, message = "Tên đệm không được vượt quá 50 ký tự")
    private String middleName;

    private String phone;

    @Size(max = 200, message = "Địa chỉ không được vượt quá 200 ký tự")
    private String address;

    @Size(max = 100, message = "Thành phố không được vượt quá 100 ký tự")
    private String city;

    @Size(max = 100, message = "Bang/Tỉnh không được vượt quá 100 ký tự")
    private String state;

    @Size(max = 10, message = "Mã bưu điện không được vượt quá 10 ký tự")
    private String zipCode;

    private String avatarUrl;

    private Gender gender;

    @Size(max = 2, message = "Mã quốc gia không được vượt quá 2 ký tự")
    private String countryCode;
}