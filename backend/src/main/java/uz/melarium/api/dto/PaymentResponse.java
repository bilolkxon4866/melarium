package uz.melarium.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import uz.melarium.domain.Payment;

public record PaymentResponse(
        UUID id,
        UUID investmentId,
        String provider,
        String providerTransactionId,
        BigDecimal amount,
        String currency,
        String status,
        Instant paidAt,
        Instant createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getInvestmentId(), payment.getProvider(),
                payment.getProviderTransactionId(), payment.getAmount(), payment.getCurrency(),
                payment.getStatus(), payment.getPaidAt(), payment.getCreatedAt());
    }
}
