package uz.melarium.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "harvests")
public class Harvest {
    @Id
    private UUID id;
    @Column(name = "apiary_id", nullable = false) private UUID apiaryId;
    @Column(name = "colony_id") private UUID colonyId;
    @Column(name = "reported_by", nullable = false) private UUID reportedBy;
    @Column(name = "quantity_kg", nullable = false, precision = 12, scale = 3) private BigDecimal quantityKg;
    @Column(name = "harvested_on", nullable = false) private LocalDate harvestedOn;
    @Column(nullable = false, columnDefinition = "text") private String note = "";
    @Column(nullable = false, length = 24) private String status = "SUBMITTED";
    @Column(name = "reviewed_by") private UUID reviewedBy;
    @Column(name = "reviewed_at") private Instant reviewedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected Harvest() { }

    public static Harvest create(UUID apiaryId, UUID colonyId, UUID reportedBy, BigDecimal quantityKg, LocalDate harvestedOn, String note) {
        Harvest value = new Harvest();
        value.id = UUID.randomUUID();
        value.apiaryId = apiaryId;
        value.colonyId = colonyId;
        value.reportedBy = reportedBy;
        value.quantityKg = quantityKg;
        value.harvestedOn = harvestedOn;
        value.note = note == null ? "" : note.trim();
        value.status = "SUBMITTED";
        return value;
    }

    public void review(String nextStatus, UUID reviewerId) {
        this.status = nextStatus;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
    }

    @PrePersist
    void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getApiaryId() { return apiaryId; }
    public UUID getColonyId() { return colonyId; }
    public UUID getReportedBy() { return reportedBy; }
    public BigDecimal getQuantityKg() { return quantityKg; }
    public LocalDate getHarvestedOn() { return harvestedOn; }
    public String getNote() { return note; }
    public String getStatus() { return status; }
    public UUID getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
