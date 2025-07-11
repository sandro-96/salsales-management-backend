// File: src/main/java/com/example/sales/job/ImageCleanupJob.java
package com.example.sales.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
public class ImageCleanupJob {

    private static final String TEMP_FOLDER = "uploads/temp";
    private static final long EXPIRE_MINUTES = 30;

    @Scheduled(fixedRate = 15 * 60 * 1000) // mỗi 15 phút
    public void cleanUpTempImages() {
        File dir = new File(TEMP_FOLDER);
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        Instant now = Instant.now();

        for (File file : files) {
            if (file.isFile()) {
                long lastModified = file.lastModified();
                Instant modifiedTime = Instant.ofEpochMilli(lastModified);
                if (modifiedTime.isBefore(now.minus(EXPIRE_MINUTES, ChronoUnit.MINUTES))) {
                    boolean deleted = file.delete();
                    log.info("Deleted temp image {}: {}", file.getName(), deleted);
                }
            }
        }
    }
}
