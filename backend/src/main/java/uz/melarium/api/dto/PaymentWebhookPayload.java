package uz.melarium.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PaymentWebhookPayload(
        @NotBlank @Size(max = 160) String eventId,
        @NotNull UUID paymentId,
        @NotBlank @Size(max = 160) String providerTransactionId,
        @NotNull BigDecimal amount,
        @NotBlank @Pattern(regexp = "UZS") String currency,
        @NotBlank @Pattern(regexp = "SUCCESS|FAILED|CANCELLED|REFUNDED") String status
) { }
