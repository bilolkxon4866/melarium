package uz.melarium.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentInitiationResponse(
        UUID paymentId,
        UUID investmentId,
        String provider,
        BigDecimal amount,
        String currency,
        String status,
        String redirectUrl,
        Instant expiresAt
) { }
