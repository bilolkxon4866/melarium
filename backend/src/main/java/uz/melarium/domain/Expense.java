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
@Table(name = "expenses")
public class Expense {
    @Id private UUID id;
    @Column(name = "apiary_id", nullable = false) private UUID apiaryId;
    @Column(name = "colony_id") private UUID colonyId;
    @Column(name = "reported_by", nullable = false) private UUID reportedBy;
    @Column(nullable = false, length = 60) private String category;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency = "UZS";
    @Column(name = "spent_on", nullable = false) private LocalDate spentOn;
    @Column(nullable = false, columnDefinition = "text") private String note = "";
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected Expense() { }

    public static Expense create(UUID apiaryId, UUID colonyId, UUID reportedBy, String category, BigDecimal amount, LocalDate spentOn, String note) {
        Expense value = new Expense();
        value.id = UUID.randomUUID();
        value.apiaryId = apiaryId;
        value.colonyId = colonyId;
        value.reportedBy = reportedBy;
        value.category = category.trim();
        value.amount = amount;
        value.currency = "UZS";
        value.spentOn = spentOn;
        value.note = note == null ? "" : note.trim();
        return value;
    }

    @PrePersist
    void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getApiaryId() { return apiaryId; }
    public UUID getColonyId() { return colonyId; }
    public UUID getReportedBy() { return reportedBy; }
    public String getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getSpentOn() { return spentOn; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
