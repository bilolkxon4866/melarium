package uz.melarium.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PaymentInitiateRequest(
        @NotNull UUID investmentId,
        @NotBlank @Pattern(regexp = "CLICK|PAYME|UZCARD|HUMO") String provider,
        @NotBlank @Size(max = 160) String idempotencyKey
) { }
