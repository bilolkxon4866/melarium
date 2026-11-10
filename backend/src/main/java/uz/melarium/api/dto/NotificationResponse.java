package uz.melarium.api.dto;

import java.time.Instant;
import java.util.UUID;

import uz.melarium.domain.Notification;

public record NotificationResponse(UUID id, String title, String body, String kind, boolean read, Instant createdAt) {
    public static NotificationResponse from(Notification item) {
        return new NotificationResponse(item.getId(), item.getTitle(), item.getBody(), item.getKind(), item.getReadAt() != null, item.getCreatedAt());
    }
}
