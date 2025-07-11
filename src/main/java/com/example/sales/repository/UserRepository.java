// File: src/main/java/com/example/sales/repository/UserRepository.java
package com.example.sales.repository;

import com.example.sales.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    @Query("{ 'email': ?0, 'deleted': false }")
    Optional<User> findByEmail(String email);

    @Query("{ 'verificationToken': ?0, 'deleted': false }")
    Optional<User> findByVerificationToken(String token);

    @Query("{ 'resetToken': ?0, 'deleted': false }")
    Optional<User> findByResetToken(String token);
}

