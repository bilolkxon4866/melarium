package uz.melarium.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ListingUpdateRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotBlank @Size(max = 240) String location,
        @NotNull @DecimalMin(value = "0.01") BigDecimal unitPrice,
        @NotNull @Min(1) @Max(10000) Integer unitsTotal
) { }
