package uz.melarium.api.dto;

import java.time.Instant;
import java.util.UUID;

import uz.melarium.domain.Colony;

public record ColonyResponse(UUID id, UUID apiaryId, String code, Integer count, String strength, String status, String description, Instant createdAt) {
    public static ColonyResponse from(Colony colony) {
        return new ColonyResponse(colony.getId(), colony.getApiaryId(), colony.getCode(), colony.getColonyCount(), colony.getStrength(), colony.getStatus(), colony.getDescription(), colony.getCreatedAt());
    }
}
