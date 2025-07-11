// File: src/main/java/com/example/sales/repository/base/SoftDeleteRepository.java
package com.example.sales.repository.base;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface SoftDeleteRepository<T, ID> extends MongoRepository<T, ID> {
    List<T> findAllActive();
    Optional<T> findActiveById(ID id);
}
