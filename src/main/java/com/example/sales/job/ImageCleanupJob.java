// File: src/main/java/com/example/sales/job/ImageCleanupJob.java
package com.example.sales.job;

import lombok.extern.slf4j.Slf4j;

/**
 * @deprecated Không còn sử dụng kể từ khi chuyển sang upload ảnh trực tiếp lên AWS S3.
 *             Không cần dọn dẹp temp folder local nữa.
 */
@Deprecated
@Slf4j
public class ImageCleanupJob {
    // Removed - S3 upload is direct, no local temp folder needed
}
