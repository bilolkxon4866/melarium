package uz.melarium.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import uz.melarium.domain.Harvest;

public record HarvestResponse(UUID id, UUID apiaryId, UUID colonyId, UUID reportedBy, BigDecimal quantityKg, LocalDate harvestedOn, String note, String status, UUID reviewedBy, Instant reviewedAt, Instant createdAt) {
    public static HarvestResponse from(Harvest item) {
        return new HarvestResponse(item.getId(), item.getApiaryId(), item.getColonyId(), item.getReportedBy(), item.getQuantityKg(), item.getHarvestedOn(), item.getNote(), item.getStatus(), item.getReviewedBy(), item.getReviewedAt(), item.getCreatedAt());
    }
}
