package com.example.sales.util;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;

public class FileUtil {
    public static MultipartFile downloadImageAsMultipartFile(String url, String name) throws Exception {
        try (InputStream in = new URL(url).openStream()) {
            return new MockMultipartFile(
                    name,
                    name + ".jpg", // hoặc lấy extension từ Content-Type
                    "image/jpeg",
                    in
            );
        }
    }
}

