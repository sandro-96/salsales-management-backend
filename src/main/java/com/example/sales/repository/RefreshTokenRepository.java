// File: src/main/java/com/example/sales/repository/RefreshTokenRepository.java
package com.example.sales.repository;

import com.example.sales.model.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);
}