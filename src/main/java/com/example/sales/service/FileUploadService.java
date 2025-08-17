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
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    @Value("${app.upload.base-dir}")
    private String baseDir;

    @Value("${app.upload.public-url:/uploads/}")
    private String publicUrl;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/jpg",
            "image/webp",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    public String uploadTemp(MultipartFile file) {
        return upload(file, "temp");
    }

    public String upload(MultipartFile file, String folder) {
        try {
            if (file.isEmpty()) throw new BusinessException(ApiCode.VALIDATION_ERROR);
            if (file.getSize() > MAX_FILE_SIZE) throw new BusinessException(ApiCode.VALIDATION_FILE_ERROR);

            String contentType = file.getContentType();
            if (!ALLOWED_MIME_TYPES.contains(contentType)) throw new BusinessException(ApiCode.VALIDATION_FILE_ERROR);

            String filename = UUID.randomUUID() + "_" + sanitize(Objects.requireNonNull(file.getOriginalFilename()));

            Path uploadPath = Path.of(baseDir, folder).toAbsolutePath();
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());

            return publicUrl + folder + "/" + filename;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi upload file", e);
            throw new RuntimeException("Không thể upload file", e);
        }
    }

    public String move(String imageUrl, String targetFolder) {
        try {
            if (imageUrl == null || !imageUrl.startsWith(publicUrl + "temp/")) {
                return imageUrl;
            }

            String filename = Path.of(imageUrl).getFileName().toString();
            Path sourcePath = Path.of(baseDir, "temp", filename).toAbsolutePath();
            Path targetDir = Path.of(baseDir, targetFolder).toAbsolutePath();
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(filename);
            if (Files.exists(sourcePath)) {
                Files.move(sourcePath, targetPath);
            }

            return publicUrl + targetFolder + "/" + filename;

        } catch (Exception e) {
            throw new RuntimeException("Không thể di chuyển file", e);
        }
    }

    private String sanitize(String original) {
        return original.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
