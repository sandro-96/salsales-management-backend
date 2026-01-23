// File: src/main/java/com/example/sales/repository/TaxPolicyRepository.java
package com.example.sales.repository;

import com.example.sales.model.tax.TaxPolicy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaxPolicyRepository extends MongoRepository<TaxPolicy, String> {

    @Query(
            value = """
          {
            'shopId': ?0,
            'branchId': ?1,
            'active': true,
            'effectiveFrom': { $lte: ?2 },
            $or: [
              { 'effectiveTo': null },
              { 'effectiveTo': { $gte: ?2 } }
            ]
          }
          """,
            sort = "{ 'effectiveFrom': -1 }"
    )
    Optional<TaxPolicy> findEffectivePolicy(
            String shopId,
            String branchId,
            LocalDateTime atTime
    );

    @Query("""
        {
          'shopId': ?0,
          'branchId': ?1,
          'active': true,
          $and: [
            { $or: [
                { 'effectiveTo': null },
                { 'effectiveTo': { $gte: ?2 } }
            ]},
            { $or: [
                { 'effectiveFrom': null },
                { 'effectiveFrom': { $lte: ?3 } }
            ]}
          ]
        }
        """)
    List<TaxPolicy> findActiveOverlappingPolicies(
            String shopId,
            String branchId,
            LocalDateTime newFrom,
            LocalDateTime newTo
    );



    List<TaxPolicy> findAllByShopIdOrderByEffectiveFromDesc(String shopId);
}

