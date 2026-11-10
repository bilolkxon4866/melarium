package uz.melarium.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "marketplace_listings")
public class MarketplaceListing {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "beekeeper_id", nullable = false)
    private UUID beekeeperId;

    @Column(name = "apiary_id", nullable = false)
    private UUID apiaryId;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "category", nullable = false, length = 40)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ListingStatus status;

    @Column(name = "location", nullable = false, length = 240)
    private String location;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "units_total", nullable = false)
    private Integer unitsTotal;

    @Column(name = "units_available", nullable = false)
    private Integer unitsAvailable;

    @Column(name = "moderation_note", nullable = false, columnDefinition = "text")
    private String moderationNote = "";

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MarketplaceListing() {
    }

    public static MarketplaceListing create(UUID beekeeperId, UUID apiaryId, String title, String description, String category, String location, BigDecimal unitPrice, Integer unitsTotal) {
        MarketplaceListing listing = new MarketplaceListing();
        listing.id = UUID.randomUUID();
        listing.beekeeperId = beekeeperId;
        listing.apiaryId = apiaryId;
        listing.title = title.trim();
        listing.description = description == null ? "" : description.trim();
        listing.category = category;
        listing.status = ListingStatus.MODERATION;
        listing.location = location.trim();
        listing.currency = "UZS";
        listing.unitPrice = unitPrice;
        listing.unitsTotal = unitsTotal;
        listing.unitsAvailable = unitsTotal;
        return listing;
    }

    public void update(String title, String description, String location, BigDecimal unitPrice, Integer unitsTotal) {
        if (title != null) this.title = title.trim();
        if (description != null) this.description = description.trim();
        if (location != null) this.location = location.trim();
        if (unitPrice != null) this.unitPrice = unitPrice;
        if (unitsTotal != null) {
            int reserved = this.unitsTotal - this.unitsAvailable;
            if (unitsTotal < reserved) throw new IllegalArgumentException("Yangi mavjudlik band qilingan miqdordan kam bo‘lishi mumkin emas");
            this.unitsTotal = unitsTotal;
            this.unitsAvailable = unitsTotal - reserved;
        }
        this.status = ListingStatus.MODERATION;
    }

    public void changeStatus(ListingStatus nextStatus) { this.status = nextStatus; }

    public void moderate(ListingStatus nextStatus, String note, UUID reviewerId) {
        this.status = nextStatus;
        this.moderationNote = note == null ? "" : note.trim();
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
    }

    public void reserveUnits(int quantity) {
        if (quantity < 1 || quantity > unitsAvailable || status != ListingStatus.ACTIVE) {
            throw new IllegalArgumentException("Taklifda yetarli faol birlik yo‘q");
        }
        this.unitsAvailable -= quantity;
        if (unitsAvailable == 0) this.status = ListingStatus.RESERVED;
    }

    public void releaseUnits(int quantity) {
        if (quantity < 1 || unitsAvailable + quantity > unitsTotal || status == ListingStatus.SOLD) {
            throw new IllegalArgumentException("Band birliklarni tiklash amali noto‘g‘ri");
        }
        this.unitsAvailable += quantity;
        if (this.status == ListingStatus.RESERVED || this.status == ListingStatus.ACTIVE) this.status = ListingStatus.ACTIVE;
    }

    public void confirmReservedUnits() {
        if (unitsAvailable == 0) this.status = ListingStatus.SOLD;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getBeekeeperId() { return beekeeperId; }
    public UUID getApiaryId() { return apiaryId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public ListingStatus getStatus() { return status; }
    public String getLocation() { return location; }
    public String getCurrency() { return currency; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Integer getUnitsTotal() { return unitsTotal; }
    public Integer getUnitsAvailable() { return unitsAvailable; }
    public String getModerationNote() { return moderationNote; }
    public UUID getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
