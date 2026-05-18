// File: src/main/java/com/example/sales/config/TableQrTokenBackfillMigration.java
package com.example.sales.config;

import com.example.sales.model.Table;
import com.example.sales.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Idempotent backfill: với mỗi {@link Table} chưa có {@code qrToken}, gán UUID ngẫu nhiên
 * và bật {@code qrOrderingEnabled=true} mặc định. Sau khi chạy 1 lần, các bàn sẵn có sẽ có
 * QR token để shop owner sinh QR.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
@RequiredArgsConstructor
public class TableQrTokenBackfillMigration implements ApplicationRunner {

    private final TableRepository tableRepository;

    @Override
    public void run(ApplicationArguments args) {
        List<Table> tables = tableRepository.findByQrTokenIsNullAndDeletedFalse();
        if (tables.isEmpty()) {
            return;
        }
        int updated = 0;
        for (Table table : tables) {
            if (StringUtils.hasText(table.getQrToken())) continue;
            table.setQrToken(UUID.randomUUID().toString().replace("-", ""));
            if (!table.isQrOrderingEnabled()) {
                table.setQrOrderingEnabled(true);
            }
            tableRepository.save(table);
            updated++;
        }
        if (updated > 0) {
            log.info("[TableQrTokenBackfillMigration] đã sinh qrToken cho {} bàn.", updated);
        }
    }
}
