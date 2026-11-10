package uz.melarium.service;

import java.util.Map;
import java.util.UUID;

import uz.melarium.api.dto.SupportTicketRequest;
import uz.melarium.api.dto.SupportTicketResponse;
import uz.melarium.api.dto.SupportTicketStatusRequest;
import uz.melarium.audit.AuditService;
import uz.melarium.domain.SupportTicket;
import uz.melarium.repository.SupportTicketRepository;
import uz.melarium.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SupportTicketService {
    private final SupportTicketRepository repository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public SupportTicketService(SupportTicketRepository repository, AuditService auditService, NotificationService notificationService) {
        this.repository = repository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> mine(AuthenticatedUser actor, int page, int size) {
        return repository.findByUserIdOrderByCreatedAtDesc(actor.id(), PageRequest.of(page, size)).map(SupportTicketResponse::from);
    }

    @Transactional
    public SupportTicketResponse create(SupportTicketRequest body, AuthenticatedUser actor) {
        SupportTicket saved = repository.save(SupportTicket.create(actor.id(), body.category(), body.subject().trim(), body.message().trim()));
        auditService.record(actor.id(), "SUPPORT_TICKET_CREATED", "SUPPORT_TICKET", saved.getId(), "SUCCESS", Map.of("category", body.category()));
        return SupportTicketResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> adminList(String status, int page, int size) {
        Page<SupportTicket> result = status == null || status.isBlank()
                ? repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                : repository.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(page, size));
        return result.map(SupportTicketResponse::from);
    }

    @Transactional
    public SupportTicketResponse review(UUID id, SupportTicketStatusRequest body, AuthenticatedUser actor) {
        SupportTicket ticket = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Yordam so‘rovi topilmadi"));
        ticket.review(body.status(), body.adminNote());
        SupportTicket saved = repository.save(ticket);
        auditService.record(actor.id(), "SUPPORT_TICKET_REVIEWED", "SUPPORT_TICKET", id, "SUCCESS",
                Map.of("status", body.status(), "adminNote", body.adminNote() == null ? "" : body.adminNote()));
        notificationService.create(saved.getUserId(), "Yordam so‘rovi yangilandi",
                "“" + saved.getSubject() + "” so‘rovi holati: " + body.status(), "SYSTEM");
        return SupportTicketResponse.from(saved);
    }
}
