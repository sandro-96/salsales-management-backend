package com.example.sales.repository;

import com.example.sales.model.NextSequence;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface NextSequenceRepository extends MongoRepository<NextSequence, String> {
    Optional<NextSequence> findByShopIdAndPrefixAndType(String shopId, String prefix, String type);
}