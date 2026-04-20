// File: src/main/java/com/example/sales/config/BranchProductWeightStockMigration.java
package com.example.sales.config;

import com.example.sales.model.BranchProduct;
import com.example.sales.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Idempotent migration: backfill {@code BranchProduct.stockInBaseUnits = 0}
 * cho những BranchProduct thuộc Product có {@code sellByWeight = true} mà
 * field này đang {@code null} (bản ghi tồn tại từ trước khi thêm feature cân).
 *
 * <p>Chạy một lần sau khi deploy, không ảnh hưởng data hiện tại (chỉ chuyển
 * null → 0 để UI không hiển thị "Còn —"). Giá trị thực tế do admin cập nhật
 * qua endpoint {@code /inventory/import-weight}.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
@RequiredArgsConstructor
public class BranchProductWeightStockMigration implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<Product> weightProducts = mongoTemplate.find(
                Query.query(Criteria.where("sellByWeight").is(true)
                        .and("deleted").is(false)),
                Product.class
        );
        if (weightProducts.isEmpty()) {
            return;
        }

        Set<String> productIds = weightProducts.stream()
                .map(Product::getId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toCollection(HashSet::new));

        if (productIds.isEmpty()) {
            return;
        }

        // Chỉ update những record có stockInBaseUnits = null để idempotent.
        var result = mongoTemplate.updateMulti(
                Query.query(Criteria.where("productId").in(productIds)
                        .and("stockInBaseUnits").is(null)),
                new Update().set("stockInBaseUnits", 0L),
                BranchProduct.class
        );

        long modified = result.getModifiedCount();
        if (modified > 0) {
            log.info("[BranchProductWeightStockMigration] backfill stockInBaseUnits=0 cho {} BranchProduct "
                    + "(sellByWeight products: {}).", modified, productIds.size());
        }
    }
}
