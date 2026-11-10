package uz.melarium.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import uz.melarium.domain.ListingStatus;

public record ListingStatusRequest(
        @NotNull ListingStatus status,
        @Size(max = 2000) String note
) { }
