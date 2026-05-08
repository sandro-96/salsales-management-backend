package com.example.sales.repository;

import com.example.sales.model.AttendanceLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceLogRepository extends MongoRepository<AttendanceLog, String> {
    Optional<AttendanceLog> findByShopIdAndStaffRefAndWorkDateAndDeletedFalse(
            String shopId, String staffRef, LocalDate workDate);

    List<AttendanceLog> findByShopIdAndWorkDateAndDeletedFalse(String shopId, LocalDate workDate);

    List<AttendanceLog> findByShopIdAndStaffRefAndWorkDateBetweenAndDeletedFalse(
            String shopId, String staffRef, LocalDate from, LocalDate to);
}

