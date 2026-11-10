package uz.melarium.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import uz.melarium.domain.Investment;
import uz.melarium.domain.MarketplaceListing;

public record InvestmentResponse(
        UUID id,
        UUID listingId,
        String listingTitle,
        String location,
        int unitsQuantity,
        BigDecimal amount,
        String currency,
        String status,
        Instant reservationExpiresAt,
        Instant createdAt
) {
    public static InvestmentResponse from(Investment investment, MarketplaceListing listing) {
        return new InvestmentResponse(investment.getId(), investment.getListingId(), listing.getTitle(), listing.getLocation(),
                investment.getUnitsQuantity(), investment.getPrincipalAmount(), investment.getCurrency(),
                investment.getStatus(), investment.getReservationExpiresAt(), investment.getCreatedAt());
    }
}
