package uz.melarium.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HarvestRequest(
        @NotNull UUID apiaryId,
        UUID colonyId,
        @NotNull @DecimalMin(value = "0.001") BigDecimal quantityKg,
        @NotNull LocalDate harvestedOn,
        @Size(max = 4000) String note
) { }
