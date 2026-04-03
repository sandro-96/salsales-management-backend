package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.NotificationType;
import com.example.sales.constant.TicketStatus;
import com.example.sales.dto.support.*;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.SupportTicket;
import com.example.sales.model.TicketReply;
import com.example.sales.model.User;
import com.example.sales.repository.SupportTicketRepository;
import com.example.sales.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public TicketResponse createTicket(String shopId, CreateTicketRequest request, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.USER_NOT_FOUND));

        SupportTicket ticket = SupportTicket.builder()
                .shopId(shopId)
                .userId(userId)
                .userEmail(user.getEmail())
                .userName(buildFullName(user))
                .subject(request.getSubject())
                .message(request.getMessage())
                .category(request.getCategory())
                .priority(request.getPriority())
                .status(TicketStatus.OPEN)
                .replies(new ArrayList<>())
                .build();

        ticket = ticketRepository.save(ticket);

        auditLogService.log(userId, shopId, ticket.getId(), "SUPPORT_TICKET", "CREATE",
                "Created support ticket: " + request.getSubject());

        return toResponse(ticket);
    }

    public Page<TicketResponse> getTickets(String shopId, String status, String category,
                                           String keyword, Pageable pageable) {
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("shopId").is(shopId));
        criteriaList.add(Criteria.where("deleted").is(false));

        if (status != null && !status.isBlank()) {
            criteriaList.add(Criteria.where("status").is(TicketStatus.valueOf(status)));
        }
        if (category != null && !category.isBlank()) {
            criteriaList.add(Criteria.where("category").is(category));
        }
        if (keyword != null && !keyword.isBlank()) {
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("subject").regex(keyword, "i"),
                    Criteria.where("userName").regex(keyword, "i"),
                    Criteria.where("userEmail").regex(keyword, "i")
            ));
        }

        Query query = new Query(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        long total = mongoTemplate.count(query, SupportTicket.class);

        query.with(pageable);
        List<SupportTicket> tickets = mongoTemplate.find(query, SupportTicket.class);

        return PageableExecutionUtils.getPage(
                tickets.stream().map(this::toResponse).toList(),
                pageable, () -> total);
    }

    public Page<TicketResponse> getMyTickets(String shopId, String userId, Pageable pageable) {
        return ticketRepository.findByShopIdAndUserIdAndDeletedFalse(shopId, userId, pageable)
                .map(this::toResponse);
    }

    public TicketResponse getTicket(String shopId, String ticketId) {
        SupportTicket ticket = findTicket(shopId, ticketId);
        return toResponse(ticket);
    }

    public TicketResponse replyToTicket(String shopId, String ticketId,
                                        ReplyTicketRequest request, String userId) {
        SupportTicket ticket = findTicket(shopId, ticketId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.USER_NOT_FOUND));

        TicketReply reply = TicketReply.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .userName(buildFullName(user))
                .message(request.getMessage())
                .createdAt(LocalDateTime.now())
                .build();

        ticket.getReplies().add(reply);

        if (ticket.getStatus() == TicketStatus.OPEN && !ticket.getUserId().equals(userId)) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }

        ticket = ticketRepository.save(ticket);

        auditLogService.log(userId, shopId, ticket.getId(), "SUPPORT_TICKET", "REPLY",
                "Replied to ticket: " + ticket.getSubject());

        String recipientId = ticket.getUserId().equals(userId)
                ? ticket.getAssigneeId()
                : ticket.getUserId();
        if (recipientId != null) {
            notificationService.send(shopId, recipientId,
                    NotificationType.TICKET_REPLIED,
                    "Phản hồi mới trên ticket",
                    reply.getUserName() + " đã phản hồi: " + ticket.getSubject(),
                    ticket.getId(), "TICKET",
                    userId, reply.getUserName());
        }

        return toResponse(ticket);
    }

    public TicketResponse updateTicketStatus(String shopId, String ticketId,
                                             UpdateTicketStatusRequest request, String userId) {
        SupportTicket ticket = findTicket(shopId, ticketId);

        ticket.setStatus(request.getStatus());
        if (request.getAssigneeId() != null) {
            ticket.setAssigneeId(request.getAssigneeId());
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.USER_NOT_FOUND));
            ticket.setAssigneeName(buildFullName(assignee));
        }

        ticket = ticketRepository.save(ticket);

        auditLogService.log(userId, shopId, ticket.getId(), "SUPPORT_TICKET", "UPDATE_STATUS",
                "Updated ticket status to " + request.getStatus());

        if (!ticket.getUserId().equals(userId)) {
            notificationService.send(shopId, ticket.getUserId(),
                    NotificationType.TICKET_STATUS_CHANGED,
                    "Trạng thái ticket đã thay đổi",
                    "Ticket \"" + ticket.getSubject() + "\" đã chuyển sang " + request.getStatus(),
                    ticket.getId(), "TICKET",
                    userId, null);
        }

        return toResponse(ticket);
    }

    public void deleteTicket(String shopId, String ticketId, String userId) {
        SupportTicket ticket = findTicket(shopId, ticketId);
        ticket.setDeleted(true);
        ticket.setDeletedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        auditLogService.log(userId, shopId, ticketId, "SUPPORT_TICKET", "DELETE",
                "Deleted ticket: " + ticket.getSubject());
    }

    private SupportTicket findTicket(String shopId, String ticketId) {
        return ticketRepository.findByIdAndShopIdAndDeletedFalse(ticketId, shopId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TICKET_NOT_FOUND));
    }

    private TicketResponse toResponse(SupportTicket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .shopId(ticket.getShopId())
                .userId(ticket.getUserId())
                .userEmail(ticket.getUserEmail())
                .userName(ticket.getUserName())
                .subject(ticket.getSubject())
                .message(ticket.getMessage())
                .category(ticket.getCategory())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .assigneeId(ticket.getAssigneeId())
                .assigneeName(ticket.getAssigneeName())
                .replies(ticket.getReplies())
                .replyCount(ticket.getReplies() != null ? ticket.getReplies().size() : 0)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private String buildFullName(User user) {
        StringBuilder sb = new StringBuilder();
        if (user.getLastName() != null) sb.append(user.getLastName());
        if (user.getMiddleName() != null) sb.append(" ").append(user.getMiddleName());
        if (user.getFirstName() != null) sb.append(" ").append(user.getFirstName());
        return sb.toString().trim();
    }
}
