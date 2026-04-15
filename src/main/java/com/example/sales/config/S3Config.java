package com.example.sales.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;

import java.net.URI;

@Slf4j
@Configuration
public class S3Config {

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Bean
    public S3Client s3Client() {
        if (accessKey == null || accessKey.isBlank()
                || secretKey == null || secretKey.isBlank()
                || bucket == null || bucket.isBlank()) {
            log.warn("Thiếu cấu hình AWS S3 (accessKey/secretKey/bucket). S3Client sẽ chạy ở chế độ no-op (các thao tác upload sẽ thất bại).");
            return S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(AnonymousCredentialsProvider.create())
                    .build();
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

        // Dùng us-east-1 làm "discovery client" để lấy region thực của bucket
        S3Client discoveryClient = S3Client.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create("https://s3.amazonaws.com"))
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(false)
                        .build())
                .build();

        String actualRegion = region;
        try {
            String locationConstraint = discoveryClient.getBucketLocation(
                    GetBucketLocationRequest.builder().bucket(bucket).build()
            ).locationConstraintAsString();
            // us-east-1 trả về empty string
            if (locationConstraint != null && !locationConstraint.isBlank()) {
                actualRegion = locationConstraint;
            } else {
                actualRegion = "us-east-1";
            }
            log.info("S3 bucket '{}' nằm ở region: {}", bucket, actualRegion);
        } catch (Exception e) {
            log.warn("Không thể tự detect region của bucket, dùng region cấu hình: {}. Lỗi: {}", region, e.getMessage());
        }

        S3Client client = S3Client.builder()
                .region(Region.of(actualRegion))
                .endpointOverride(URI.create("https://s3." + actualRegion + ".amazonaws.com"))
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(false)
                        .build())
                .build();

        // Lưu lại actualRegion để các bean khác có thể dùng
        System.setProperty("aws.s3.actual-region", actualRegion);

        return client;
    }
}

