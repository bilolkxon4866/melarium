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
@Table(name = "investments")
public class Investment {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "investor_id", nullable = false, updatable = false)
    private UUID investorId;

    @Column(name = "listing_id", nullable = false, updatable = false)
    private UUID listingId;

    @Column(name = "units_quantity", nullable = false, updatable = false)
    private Integer unitsQuantity;

    @Column(name = "principal_amount", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal principalAmount;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency = "UZS";

    @Column(nullable = false, length = 24)
    private String status = "PENDING_PAYMENT";

    @Column(name = "terms_version", length = 40)
    private String termsVersion;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Column(name = "reservation_expires_at")
    private Instant reservationExpiresAt;

    @Column(name = "idempotency_key", length = 120, updatable = false)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Investment() { }

    public static Investment create(UUID investorId, UUID listingId, int unitsQuantity, BigDecimal principalAmount,
                                    Instant reservationExpiresAt, String idempotencyKey,
                                    String termsVersion, Instant termsAcceptedAt) {
        Investment value = new Investment();
        value.id = UUID.randomUUID();
        value.investorId = investorId;
        value.listingId = listingId;
        value.unitsQuantity = unitsQuantity;
        value.principalAmount = principalAmount;
        value.currency = "UZS";
        value.status = "PENDING_PAYMENT";
        value.reservationExpiresAt = reservationExpiresAt;
        value.idempotencyKey = idempotencyKey;
        value.termsVersion = termsVersion;
        value.termsAcceptedAt = termsAcceptedAt;
        return value;
    }

    public void activate() {
        if (!"PENDING_PAYMENT".equals(status)) throw new IllegalStateException("Faqat kutilayotgan buyurtmani faollashtirish mumkin");
        status = "ACTIVE";
        reservationExpiresAt = null;
    }

    public void cancel() {
        if (!"PENDING_PAYMENT".equals(status)) throw new IllegalStateException("Faqat to‘lov kutilayotgan buyurtmani bekor qilish mumkin");
        status = "CANCELLED";
        reservationExpiresAt = null;
    }

    public void expire() {
        if (!"PENDING_PAYMENT".equals(status)) throw new IllegalStateException("Faqat to‘lov kutilayotgan buyurtma muddati tugashi mumkin");
        status = "CANCELLED";
        reservationExpiresAt = null;
    }

    public void markPaymentReview() {
        if (!java.util.Set.of("PENDING_PAYMENT", "CANCELLED").contains(status)) throw new IllegalStateException("To‘lov tekshiruvi holati noto‘g‘ri");
        status = "PAYMENT_REVIEW";
        reservationExpiresAt = null;
    }

    public void markRefunded() {
        if (!java.util.Set.of("ACTIVE", "PAYMENT_REVIEW", "PENDING_PAYMENT").contains(status)) throw new IllegalStateException("Qaytarilgan investitsiya holati noto‘g‘ri");
        status = "REFUNDED";
        reservationExpiresAt = null;
    }

    public boolean reservationExpired(Instant now) {
        return "PENDING_PAYMENT".equals(status) && reservationExpiresAt != null && !reservationExpiresAt.isAfter(now);
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
    public UUID getInvestorId() { return investorId; }
    public UUID getListingId() { return listingId; }
    public Integer getUnitsQuantity() { return unitsQuantity; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getTermsVersion() { return termsVersion; }
    public Instant getTermsAcceptedAt() { return termsAcceptedAt; }
    public Instant getReservationExpiresAt() { return reservationExpiresAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
