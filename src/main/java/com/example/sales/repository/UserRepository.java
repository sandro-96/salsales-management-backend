// File: src/main/java/com/example/sales/repository/UserRepository.java
package com.example.sales.repository;

import com.example.sales.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmailAndDeletedFalse(String email);
    Optional<User> findByIdAndDeletedFalse(String id);
    Optional<User> findByVerificationTokenAndDeletedFalse(String token);
}

