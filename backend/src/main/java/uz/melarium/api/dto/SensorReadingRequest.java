package uz.melarium.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SensorReadingRequest(
        @NotNull UUID apiaryId,
        UUID colonyId,
        @NotBlank @Pattern(regexp = "TEMPERATURE|HUMIDITY|WEIGHT") String metric,
        @NotNull @DecimalMin(value = "-50") @DecimalMax(value = "1000") BigDecimal value,
        @NotBlank @Size(max = 16) String unit,
        @NotBlank @Pattern(regexp = "MANUAL|SIMULATED") String source,
        @NotNull Instant recordedAt
) { }
