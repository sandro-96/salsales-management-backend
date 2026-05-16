package com.example.sales.service;

import com.example.sales.constant.ApiCode;
import com.example.sales.constant.NotificationType;
import com.example.sales.constant.TicketPriority;
import com.example.sales.constant.TicketStatus;
import com.example.sales.constant.UserRole;
import com.example.sales.dto.support.*;
import com.example.sales.exception.ResourceNotFoundException;
import com.example.sales.model.Shop;
import com.example.sales.model.SupportTicket;
import com.example.sales.model.TicketReply;
import com.example.sales.model.User;
import com.example.sales.repository.ShopRepository;
import com.example.sales.repository.SupportTicketRepository;
import com.example.sales.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final MongoTemplate mongoTemplate;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final SupportRecipientResolver recipientResolver;
    private final MailService mailService;

    @Value("${app.fe.url:}")
    private String frontendUrl;

    /** Hỗ trợ user ↔ admin; không gắn shop ({@code shopId} lưu null). */
    public TicketResponse createUserTicket(CreateTicketRequest request, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.USER_NOT_FOUND));

        SupportTicket ticket = SupportTicket.builder()
                .shopId(null)
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

        auditLogService.log(userId, null, ticket.getId(), "SUPPORT_TICKET", "CREATE",
                "Created support ticket: " + request.getSubject());

        fanOutTicketCreated(ticket, user);

        return toResponse(ticket);
    }

    public Page<TicketResponse> listUserTickets(String userId, String status, String category,
                                                 String keyword, Pageable pageable) {
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("userId").is(userId));
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

    public TicketResponse getUserTicket(String userId, String ticketId) {
        SupportTicket ticket = findTicketOwnedByUser(ticketId, userId);
        return toResponse(ticket);
    }

    public TicketResponse replyUserTicket(String ticketId, ReplyTicketRequest request, String userId) {
        SupportTicket ticket = findTicketOwnedByUser(ticketId, userId);
        return applyReply(ticket, request, userId);
    }

    public TicketResponse replyToTicket(String shopId, String ticketId,
                                        ReplyTicketRequest request, String userId) {
        SupportTicket ticket = findTicket(shopId, ticketId);
        return applyReply(ticket, request, userId);
    }

    private TicketResponse applyReply(SupportTicket ticket, ReplyTicketRequest request, String userId) {
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

        auditLogService.log(userId, ticket.getShopId(), ticket.getId(), "SUPPORT_TICKET", "REPLY",
                "Replied to ticket: " + ticket.getSubject());

        fanOutReply(ticket, user, reply);

        return toResponse(ticket);
    }

    public TicketResponse updateUserTicketStatus(String ticketId, UpdateTicketStatusRequest request,
                                                 String userId) {
        SupportTicket ticket = findTicketOwnedByUser(ticketId, userId);
        TicketStatus previousStatus = ticket.getStatus();

        ticket.setStatus(request.getStatus());

        ticket = ticketRepository.save(ticket);

        auditLogService.log(userId, null, ticket.getId(), "SUPPORT_TICKET", "UPDATE_STATUS",
                "Updated ticket status to " + request.getStatus());

        fanOutStatusChange(ticket, userId, previousStatus, request.getStatus());

        return toResponse(ticket);
    }

    public TicketResponse updateTicketStatus(String shopId, String ticketId,
                                             UpdateTicketStatusRequest request, String userId) {
        SupportTicket ticket = findTicket(shopId, ticketId);
        TicketStatus previousStatus = ticket.getStatus();

        ticket.setStatus(request.getStatus());
        if (request.getAssigneeId() != null) {
            ticket.setAssigneeId(request.getAssigneeId());
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.USER_NOT_FOUND));
            ticket.setAssigneeName(buildFullName(assignee));
        }

        ticket = ticketRepository.save(ticket);

        auditLogService.log(userId, ticket.getShopId(), ticket.getId(), "SUPPORT_TICKET", "UPDATE_STATUS",
                "Updated ticket status to " + request.getStatus());

        fanOutStatusChange(ticket, userId, previousStatus, request.getStatus());

        return toResponse(ticket);
    }

    public void deleteUserTicket(String ticketId, String userId) {
        SupportTicket ticket = findTicketOwnedByUser(ticketId, userId);
        ticket.setDeleted(true);
        ticket.setDeletedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        auditLogService.log(userId, null, ticketId, "SUPPORT_TICKET", "DELETE",
                "Deleted ticket: " + ticket.getSubject());
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Admin (cross-shop) operations — dùng cho /api/admin/support
    // ────────────────────────────────────────────────────────────────────────────

    public Page<TicketResponse> adminListTickets(String status, String priority, String shopId,
                                                 String assigneeId, String keyword, Pageable pageable) {
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("deleted").is(false));

        if (status != null && !status.isBlank()) {
            criteriaList.add(Criteria.where("status").is(TicketStatus.valueOf(status)));
        }
        if (priority != null && !priority.isBlank()) {
            criteriaList.add(Criteria.where("priority").is(TicketPriority.valueOf(priority)));
        }
        if (shopId != null && !shopId.isBlank()) {
            criteriaList.add(Criteria.where("shopId").is(shopId));
        }
        if (assigneeId != null && !assigneeId.isBlank()) {
            criteriaList.add(Criteria.where("assigneeId").is(assigneeId));
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

    public TicketResponse adminGetTicket(String ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TICKET_NOT_FOUND));
        return toResponse(ticket);
    }

    public TicketResponse adminReply(String ticketId, ReplyTicketRequest request, String adminUserId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TICKET_NOT_FOUND));
        return replyToTicket(ticket.getShopId(), ticket.getId(), request, adminUserId);
    }

    public TicketResponse adminUpdateStatus(String ticketId, UpdateTicketStatusRequest request,
                                            String adminUserId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TICKET_NOT_FOUND));

        // Nếu admin chưa được assign và không truyền assigneeId → tự gán chính admin hiện tại
        if ((ticket.getAssigneeId() == null || ticket.getAssigneeId().isBlank())
                && (request.getAssigneeId() == null || request.getAssigneeId().isBlank())) {
            request.setAssigneeId(adminUserId);
        }

        return updateTicketStatus(ticket.getShopId(), ticket.getId(), request, adminUserId);
    }

    public Map<String, Long> adminStats() {
        Map<String, Long> stats = new HashMap<>();
        for (TicketStatus st : TicketStatus.values()) {
            Query q = new Query(new Criteria().andOperator(
                    Criteria.where("deleted").is(false),
                    Criteria.where("status").is(st)
            ));
            stats.put(st.name(), mongoTemplate.count(q, SupportTicket.class));
        }
        return stats;
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Fan-out helpers
    // ────────────────────────────────────────────────────────────────────────────

    private void fanOutTicketCreated(SupportTicket ticket, User creator) {
        List<String> adminIds = recipientResolver.getAdminUserIds().stream()
                .filter(id -> !Objects.equals(id, creator.getId()))
                .toList();

        if (!adminIds.isEmpty()) {
            Map<String, Object> data = new HashMap<>();
            data.put("titleKey", "TICKET_CREATED");
            data.put("messageKey", "TICKET_CREATED");
            data.put("actor", ticket.getUserName());
            data.put("subject", ticket.getSubject());
            notificationService.sendToMultiple(
                    ticket.getShopId(), adminIds,
                    NotificationType.TICKET_CREATED,
                    null, null, data,
                    ticket.getId(), "TICKET",
                    creator.getId(), ticket.getUserName());
        }

        if (isHighPriority(ticket.getPriority())) {
            sendTicketCreatedEmailsAsync(ticket, creator);
        }
    }

    private void fanOutReply(SupportTicket ticket, User replier, TicketReply reply) {
        boolean replierIsAdmin = replier.getRole() == UserRole.ROLE_ADMIN;
        boolean replierIsCreator = ticket.getUserId().equals(replier.getId());

        if (replierIsCreator) {
            // Người tạo ticket phản hồi — ưu tiên báo assignee; nếu chưa assign thì fan-out tất cả admin.
            String assigneeId = ticket.getAssigneeId();
            Map<String, Object> replyData = ticketReplyTemplateData(
                    reply.getUserName(), ticket.getSubject(), false);
            if (assigneeId != null && !assigneeId.isBlank()) {
                notificationService.send(ticket.getShopId(), assigneeId,
                        NotificationType.TICKET_REPLIED,
                        null, null, replyData,
                        ticket.getId(), "TICKET",
                        replier.getId(), reply.getUserName());
            } else {
                List<String> adminIds = recipientResolver.getAdminUserIds().stream()
                        .filter(id -> !Objects.equals(id, replier.getId()))
                        .toList();
                if (!adminIds.isEmpty()) {
                    Map<String, Object> unassignedData = ticketReplyTemplateData(
                            reply.getUserName(), ticket.getSubject(), true);
                    notificationService.sendToMultiple(ticket.getShopId(), adminIds,
                            NotificationType.TICKET_REPLIED,
                            null, null, unassignedData,
                            ticket.getId(), "TICKET",
                            replier.getId(), reply.getUserName());
                }
            }
        } else {
            Map<String, Object> replyData = ticketReplyTemplateData(
                    reply.getUserName(), ticket.getSubject(), false);
            notificationService.send(ticket.getShopId(), ticket.getUserId(),
                    NotificationType.TICKET_REPLIED,
                    null, null, replyData,
                    ticket.getId(), "TICKET",
                    replier.getId(), reply.getUserName());

            if (replierIsAdmin && isHighPriority(ticket.getPriority())
                    && ticket.getUserEmail() != null && !ticket.getUserEmail().isBlank()) {
                sendTicketReplyEmailAsync(ticket, reply);
            }
        }
    }

    private void fanOutStatusChange(SupportTicket ticket, String actorId,
                                    TicketStatus previous, TicketStatus next) {
        if (previous == next) return;
        if (ticket.getUserId().equals(actorId)) return;

        Map<String, Object> statusData = new HashMap<>();
        statusData.put("titleKey", "TICKET_STATUS_CHANGED");
        statusData.put("messageKey", "TICKET_STATUS_CHANGED");
        statusData.put("subject", ticket.getSubject());
        statusData.put("status", next.name());
        notificationService.send(ticket.getShopId(), ticket.getUserId(),
                NotificationType.TICKET_STATUS_CHANGED,
                null, null, statusData,
                ticket.getId(), "TICKET",
                actorId, null);

        boolean terminal = next == TicketStatus.RESOLVED || next == TicketStatus.CLOSED;
        if (terminal && isHighPriority(ticket.getPriority())
                && ticket.getUserEmail() != null && !ticket.getUserEmail().isBlank()) {
            sendTicketStatusEmailAsync(ticket, next);
        }
    }

    private static boolean isHighPriority(TicketPriority priority) {
        return priority == TicketPriority.HIGH || priority == TicketPriority.URGENT;
    }

    private static Map<String, Object> ticketReplyTemplateData(
            String actor, String subject, boolean unassigned) {
        Map<String, Object> data = new HashMap<>();
        data.put("titleKey", unassigned ? "TICKET_REPLIED_UNASSIGNED" : "TICKET_REPLIED");
        data.put("messageKey", "TICKET_REPLIED");
        data.put("actor", actor);
        data.put("subject", subject);
        return data;
    }

    private void sendTicketCreatedEmailsAsync(SupportTicket ticket, User creator) {
        try {
            List<User> admins = recipientResolver.getAdminUsers();
            if (admins.isEmpty()) return;

            String shopName = resolveShopName(ticket.getShopId());
            String ticketUrl = buildAdminTicketUrl(ticket.getId());

            for (User admin : admins) {
                if (admin.getEmail() == null || admin.getEmail().isBlank()) continue;
                if (Objects.equals(admin.getId(), creator.getId())) continue;

                Map<String, Object> model = new HashMap<>();
                model.put("adminName", buildFullName(admin));
                model.put("shopName", shopName);
                model.put("creatorName", ticket.getUserName());
                model.put("creatorEmail", ticket.getUserEmail());
                model.put("subject", ticket.getSubject());
                model.put("priority", ticket.getPriority() != null ? ticket.getPriority().name() : "");
                model.put("category", ticket.getCategory() != null ? ticket.getCategory().name() : "");
                model.put("message", ticket.getMessage());
                model.put("ticketUrl", ticketUrl);

                mailService.sendTicketEmailAsync(
                        admin.getEmail(),
                        "[Hỗ trợ] " + ticket.getPriority() + " — " + ticket.getSubject(),
                        "emails/ticket-created-admin",
                        model);
            }
        } catch (Exception ex) {
            log.warn("Failed to send ticket-created emails for ticket {}: {}",
                    ticket.getId(), ex.getMessage());
        }
    }

    private void sendTicketReplyEmailAsync(SupportTicket ticket, TicketReply reply) {
        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", ticket.getUserName());
        model.put("subject", ticket.getSubject());
        model.put("replierName", reply.getUserName());
        model.put("replyMessage", reply.getMessage());
        model.put("priority", ticket.getPriority() != null ? ticket.getPriority().name() : "");
        model.put("ticketUrl", buildShopTicketUrl(ticket.getId()));

        mailService.sendTicketEmailAsync(
                ticket.getUserEmail(),
                "[Hỗ trợ] Phản hồi mới — " + ticket.getSubject(),
                "emails/ticket-replied",
                model);
    }

    private void sendTicketStatusEmailAsync(SupportTicket ticket, TicketStatus next) {
        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", ticket.getUserName());
        model.put("subject", ticket.getSubject());
        model.put("status", next.name());
        model.put("ticketUrl", buildShopTicketUrl(ticket.getId()));

        mailService.sendTicketEmailAsync(
                ticket.getUserEmail(),
                "[Hỗ trợ] Cập nhật trạng thái — " + ticket.getSubject(),
                "emails/ticket-status-changed",
                model);
    }

    private String resolveShopName(String shopId) {
        if (shopId == null) return "";
        return shopRepository.findByIdAndDeletedFalse(shopId)
                .map(Shop::getName)
                .orElse(shopId);
    }

    private String buildAdminTicketUrl(String ticketId) {
        String base = frontendUrl == null ? "" : frontendUrl;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/admin/support?ticketId=" + ticketId;
    }

    private String buildShopTicketUrl(String ticketId) {
        String base = frontendUrl == null ? "" : frontendUrl;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/support?ticketId=" + ticketId;
    }

    // ────────────────────────────────────────────────────────────────────────────

    private SupportTicket findTicket(String shopId, String ticketId) {
        if (StringUtils.hasText(shopId)) {
            return ticketRepository.findByIdAndShopIdAndDeletedFalse(ticketId, shopId.trim())
                    .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TICKET_NOT_FOUND));
        }
        return ticketRepository.findById(ticketId)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TICKET_NOT_FOUND));
    }

    private SupportTicket findTicketOwnedByUser(String ticketId, String userId) {
        return ticketRepository.findByIdAndUserIdAndDeletedFalse(ticketId, userId)
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
