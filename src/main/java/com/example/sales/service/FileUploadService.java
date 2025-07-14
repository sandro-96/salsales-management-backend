// File: src/main/java/com/example/sales/service/FileUploadService.java
package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    @Value("${app.upload.temp-dir:uploads/temp/}")
    private String tempDir;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    public String upload(MultipartFile file) {
        try {
            log.info("Bắt đầu upload file: tên gốc = {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                throw new BusinessException(ApiCode.VALIDATION_ERROR);
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new BusinessException(ApiCode.VALIDATION_FILE_ERROR);
            }

            String contentType = file.getContentType();
            if (!ALLOWED_MIME_TYPES.contains(contentType)) {
                throw new BusinessException(ApiCode.VALIDATION_FILE_ERROR);
            }

            // Tạo tên file ngẫu nhiên
            String filename = UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());

            // Đường dẫn thư mục uploads/temp/
            Path uploadPath = Path.of(tempDir).toAbsolutePath();
            Files.createDirectories(uploadPath);
            log.debug("Thư mục upload: {}", uploadPath);

            // Ghi file vào disk
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath);
            log.info("File đã được lưu tại: {}", filePath);

            // Trả về URL public
            return "/uploads/temp/" + filename;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi upload file", e);
            throw new RuntimeException("Không thể upload file", e);
        }
    }

    // Dọn tên file cho an toàn
    private String sanitize(String original) {
        String sanitized = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        log.debug("Sanitize tên file: {} -> {}", original, sanitized);
        return sanitized;
    }

    public String moveToProduct(String imageUrl) {
        try {
            log.info("Di chuyển file từ temp sang product: {}", imageUrl);

            // Chỉ xử lý nếu là ảnh trong uploads/temp
            if (imageUrl == null || !imageUrl.startsWith("/uploads/temp/")) {
                log.warn("Bỏ qua file không thuộc temp: {}", imageUrl);
                return imageUrl; // đã là ảnh final hoặc ảnh CDN thì bỏ qua
            }

            String filename = Path.of(imageUrl).getFileName().toString();

            Path tempPath = Path.of(tempDir).resolve(filename).toAbsolutePath();
            Path productDir = Path.of("uploads/product").toAbsolutePath();
            Files.createDirectories(productDir);
            Path targetPath = productDir.resolve(filename);

            if (Files.exists(tempPath)) {
                Files.move(tempPath, targetPath);
                log.info("Đã chuyển file: {} -> {}", tempPath, targetPath);
            } else {
                log.warn("File không tồn tại tại tempPath: {}", tempPath);
            }

            return "/uploads/product/" + filename;

        } catch (Exception e) {
            log.error("Lỗi khi di chuyển ảnh từ temp sang product", e);
            throw new RuntimeException("Không thể chuyển ảnh từ temp sang product", e);
        }
    }

}
