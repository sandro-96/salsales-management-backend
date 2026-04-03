package com.example.sales.repository;

import com.example.sales.constant.TicketStatus;
import com.example.sales.model.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SupportTicketRepository extends MongoRepository<SupportTicket, String> {

    Page<SupportTicket> findByShopIdAndDeletedFalse(String shopId, Pageable pageable);

    Page<SupportTicket> findByShopIdAndStatusAndDeletedFalse(String shopId, TicketStatus status, Pageable pageable);

    Page<SupportTicket> findByShopIdAndUserIdAndDeletedFalse(String shopId, String userId, Pageable pageable);

    Optional<SupportTicket> findByIdAndShopIdAndDeletedFalse(String id, String shopId);
}
