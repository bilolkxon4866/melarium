package uz.melarium.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import uz.melarium.domain.MarketplaceListing;

public record ListingResponse(
        UUID id,
        UUID beekeeperId,
        UUID apiaryId,
        String title,
        String description,
        String category,
        String status,
        String location,
        String currency,
        BigDecimal unitPrice,
        Integer unitsTotal,
        Integer unitsAvailable,
        Instant createdAt
) {
    public static ListingResponse from(MarketplaceListing listing) {
        return new ListingResponse(
                listing.getId(),
                listing.getBeekeeperId(),
                listing.getApiaryId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getCategory(),
                listing.getStatus().name().toLowerCase(),
                listing.getLocation(),
                listing.getCurrency(),
                listing.getUnitPrice(),
                listing.getUnitsTotal(),
                listing.getUnitsAvailable(),
                listing.getCreatedAt()
        );
    }
}
