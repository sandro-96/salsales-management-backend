package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/jpg",
            "image/webp",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    /**
     * Upload file lên S3 vào folder chỉ định, trả về public URL.
     */
    public String upload(MultipartFile file, String folder) {
        try {
            if (file.isEmpty()) throw new BusinessException(ApiCode.FILE_EMPTY);
            if (file.getSize() > MAX_FILE_SIZE) throw new BusinessException(ApiCode.FILE_TOO_LARGE);

            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
                throw new BusinessException(ApiCode.INVALID_FILE_TYPE);
            }

            String filename = UUID.randomUUID() + "_" + sanitize(Objects.requireNonNull(file.getOriginalFilename()));
            String key = folder + "/" + filename;

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));

            return getPublicUrl(key);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi upload file lên S3", e);
            throw new RuntimeException("Không thể upload file", e);
        }
    }

    /**
     * Xóa file trên S3 theo URL đầy đủ.
     */
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;
        try {
            String key = extractKeyFromUrl(fileUrl);
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (Exception e) {
            log.warn("Không thể xóa file trên S3: {}", fileUrl, e);
        }
    }

    // ─── helpers ───────────────────────────────────────────────────────────

    private String getPublicUrl(String key) {
        // Dùng actual region đã được detect bởi S3Config (tránh URL sai khi region khác default)
        String actualRegion = System.getProperty("aws.s3.actual-region", region);
        return "https://" + bucket + ".s3." + actualRegion + ".amazonaws.com/" + key;
    }

    private String extractKeyFromUrl(String url) {
        // Thử extract với actual region trước, fallback về configured region
        String actualRegion = System.getProperty("aws.s3.actual-region", region);
        String prefix = "https://" + bucket + ".s3." + actualRegion + ".amazonaws.com/";
        if (url.startsWith(prefix)) {
            return url.substring(prefix.length());
        }
        // fallback: thử với configured region
        String fallbackPrefix = "https://" + bucket + ".s3." + region + ".amazonaws.com/";
        if (url.startsWith(fallbackPrefix)) {
            return url.substring(fallbackPrefix.length());
        }
        return url;
    }

    private String sanitize(String original) {
        String name = original.toLowerCase();
        name = name.replaceAll("[^a-z0-9._-]", "_");
        return name.length() > 100 ? name.substring(0, 100) : name;
    }
}
