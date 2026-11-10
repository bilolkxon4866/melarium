package uz.melarium.service;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.melarium.api.dto.SupportTicketRequest;
import uz.melarium.api.dto.SupportTicketStatusRequest;
import uz.melarium.audit.AuditService;
import uz.melarium.domain.SupportTicket;
import uz.melarium.repository.SupportTicketRepository;
import uz.melarium.security.AuthenticatedUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceTest {
    @Mock SupportTicketRepository repository;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;
    SupportTicketService service;

    @BeforeEach
    void setUp() { service = new SupportTicketService(repository, auditService, notificationService); }

    @Test
    void createsPrivateSupportTicketAndAuditsIt() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser actor = new AuthenticatedUser(userId, java.util.List.of("INVESTOR"));
        when(repository.save(any(SupportTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new SupportTicketRequest("COMPLAINT", "  Test  ", "  Matn  "), actor);

        assertEquals(userId, response.userId());
        assertEquals("COMPLAINT", response.category());
        assertEquals("Test", response.subject());
        assertEquals("Matn", response.message());
        assertEquals("OPEN", response.status());
        assertNotNull(response.id());
        verify(auditService).record(eq(userId), eq("SUPPORT_TICKET_CREATED"), eq("SUPPORT_TICKET"), eq(response.id()), eq("SUCCESS"), any());
    }

    @Test
    void adminReviewNotifiesTicketOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        SupportTicket ticket = SupportTicket.create(ownerId, "TECHNICAL", "Kirish", "Kod kelmadi");
        AuthenticatedUser actor = new AuthenticatedUser(adminId, java.util.List.of("ADMIN"));
        when(repository.findById(any())).thenReturn(Optional.of(ticket));
        when(repository.save(any(SupportTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.review(UUID.randomUUID(), new SupportTicketStatusRequest("IN_REVIEW", "Ko‘rib chiqilmoqda"), actor);

        assertEquals("IN_REVIEW", response.status());
        assertEquals("Ko‘rib chiqilmoqda", response.adminNote());
        verify(notificationService).create(ownerId, "Yordam so‘rovi yangilandi", "“Kirish” so‘rovi holati: IN_REVIEW", "SYSTEM");
        verify(auditService).record(eq(adminId), eq("SUPPORT_TICKET_REVIEWED"), eq("SUPPORT_TICKET"), any(), eq("SUCCESS"), any());
    }
}
