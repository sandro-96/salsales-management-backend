package com.example.sales.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    @Value("${app.upload.temp-dir:uploads/temp/}")
    private String tempDir;

    public String upload(MultipartFile file) {
        try {
            // Tạo tên file ngẫu nhiên
            String filename = UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());

            // Đường dẫn thư mục uploads/temp/
            Path uploadPath = Path.of(tempDir).toAbsolutePath();
            Files.createDirectories(uploadPath);

            // Ghi file vào disk
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath);

            // Trả về URL public
            return "/uploads/temp/" + filename;

        } catch (Exception e) {
            throw new RuntimeException("Không thể upload file", e);
        }
    }

    // Dọn tên file cho an toàn
    private String sanitize(String original) {
        return original.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public String moveToProduct(String imageUrl) {
        try {
            // Chỉ xử lý nếu là ảnh trong uploads/temp
            if (imageUrl == null || !imageUrl.startsWith("/uploads/temp/")) {
                return imageUrl; // đã là ảnh final hoặc ảnh CDN thì bỏ qua
            }

            String filename = Path.of(imageUrl).getFileName().toString();

            Path tempPath = Path.of(tempDir).resolve(filename).toAbsolutePath();
            Path productDir = Path.of("uploads/product").toAbsolutePath();
            Files.createDirectories(productDir);
            Path targetPath = productDir.resolve(filename);

            if (Files.exists(tempPath)) {
                Files.move(tempPath, targetPath);
            }

            return "/uploads/product/" + filename;

        } catch (Exception e) {
            throw new RuntimeException("Không thể chuyển ảnh từ temp sang product", e);
        }
    }

}
