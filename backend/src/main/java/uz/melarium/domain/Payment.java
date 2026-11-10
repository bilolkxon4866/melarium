package uz.melarium.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "investment_id", nullable = false, updatable = false)
    private UUID investmentId;

    @Column(nullable = false, length = 24, updatable = false)
    private String provider;

    @Column(name = "provider_transaction_id", length = 160)
    private String providerTransactionId;

    @Column(name = "idempotency_key", length = 160, updatable = false)
    private String idempotencyKey;

    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency = "UZS";

    @Column(nullable = false, length = 24)
    private String status = "PENDING";

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() { }

    public static Payment create(UUID investmentId, String provider, String idempotencyKey, BigDecimal amount) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.investmentId = investmentId;
        payment.provider = provider;
        payment.idempotencyKey = idempotencyKey;
        payment.amount = amount;
        payment.currency = "UZS";
        payment.status = "PENDING";
        return payment;
    }

    public void applyProviderResult(String nextStatus, String transactionId, Instant paidAt) {
        this.status = nextStatus;
        if (transactionId != null && !transactionId.isBlank()) this.providerTransactionId = transactionId;
        this.paidAt = "SUCCESS".equals(nextStatus) ? paidAt : null;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getInvestmentId() { return investmentId; }
    public String getProvider() { return provider; }
    public String getProviderTransactionId() { return providerTransactionId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
