// File: src/main/java/com/example/sales/repository/base/SoftDeleteRepositoryImpl.java
package com.example.sales.repository.base;

import com.example.sales.model.base.BaseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

public abstract class SoftDeleteRepositoryImpl<T extends BaseEntity, ID> implements SoftDeleteRepository<T, ID> {

    @Autowired
    protected MongoTemplate mongoTemplate;

    private final Class<T> entityClass;

    public SoftDeleteRepositoryImpl(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public List<T> findAllActive() {
        return mongoTemplate.find(Query.query(Criteria.where("deleted").is(false)), entityClass);
    }

    @Override
    public Optional<T> findActiveById(ID id) {
        Query query = new Query(Criteria.where("id").is(id).and("deleted").is(false));
        return Optional.of(mongoTemplate.findOne(query, entityClass));
    }
}
