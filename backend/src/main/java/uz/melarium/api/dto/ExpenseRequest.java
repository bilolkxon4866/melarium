package uz.melarium.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExpenseRequest(
        @NotNull UUID apiaryId,
        UUID colonyId,
        @NotBlank @Size(max = 60) String category,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull LocalDate spentOn,
        @Size(max = 4000) String note
) { }
