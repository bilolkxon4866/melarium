package uz.melarium.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateInvestmentRequest(
        @NotNull UUID listingId,
        @NotNull @Min(1) @Max(1000) Integer unitsQuantity,
        @Size(max = 120) String idempotencyKey,
        Boolean acceptTerms,
        @Size(max = 40) String termsVersion
) { }
