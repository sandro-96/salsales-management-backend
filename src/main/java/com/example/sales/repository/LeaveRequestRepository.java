package com.example.sales.repository;

import com.example.sales.constant.LeaveRequestStatus;
import com.example.sales.model.LeaveRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRequestRepository extends MongoRepository<LeaveRequest, String> {
    Optional<LeaveRequest> findByIdAndShopIdAndDeletedFalse(String id, String shopId);

    List<LeaveRequest> findByShopIdAndStaffRefAndDeletedFalse(String shopId, String staffRef);

    List<LeaveRequest> findByShopIdAndStaffRefAndFromDateGreaterThanEqualAndToDateLessThanEqualAndDeletedFalse(
            String shopId, String staffRef, LocalDate from, LocalDate to);

    List<LeaveRequest> findByShopIdAndFromDateGreaterThanEqualAndToDateLessThanEqualAndDeletedFalse(
            String shopId, LocalDate from, LocalDate to);

    long countByShopIdAndStatusAndDeletedFalse(String shopId, LeaveRequestStatus status);
}

