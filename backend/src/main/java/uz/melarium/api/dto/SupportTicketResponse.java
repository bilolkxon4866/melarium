package uz.melarium.api.dto;

import java.time.Instant;
import java.util.UUID;

import uz.melarium.domain.SupportTicket;

public record SupportTicketResponse(
        UUID id,
        UUID userId,
        String category,
        String subject,
        String message,
        String status,
        String adminNote,
        Instant createdAt,
        Instant updatedAt
) {
    public static SupportTicketResponse from(SupportTicket item) {
        return new SupportTicketResponse(item.getId(), item.getUserId(), item.getCategory(), item.getSubject(),
                item.getMessage(), item.getStatus(), item.getAdminNote(), item.getCreatedAt(), item.getUpdatedAt());
    }
}
