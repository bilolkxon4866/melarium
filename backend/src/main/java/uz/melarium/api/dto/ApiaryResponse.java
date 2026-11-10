package uz.melarium.api.dto;

import java.time.Instant;
import java.util.UUID;

import uz.melarium.domain.Apiary;

public record ApiaryResponse(UUID id, UUID ownerId, String name, String location, String description, String status, Instant createdAt) {
    public static ApiaryResponse from(Apiary apiary) {
        return new ApiaryResponse(apiary.getId(), apiary.getOwnerId(), apiary.getName(), apiary.getLocation(), apiary.getDescription(), apiary.getStatus(), apiary.getCreatedAt());
    }
}
