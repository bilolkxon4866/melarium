package uz.melarium.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "colonies")
public class Colony {
    @Id
    private UUID id;

    @Column(name = "apiary_id", nullable = false)
    private UUID apiaryId;

    @Column(length = 40)
    private String code;

    @Column(name = "colony_count", nullable = false)
    private Integer colonyCount = 1;

    @Column(length = 24)
    private String strength;

    @Column(nullable = false, length = 24)
    private String status = "ACTIVE";

    @Column(nullable = false, columnDefinition = "text")
    private String description = "";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Colony() { }

    public static Colony create(UUID apiaryId, String code, Integer colonyCount, String strength, String description) {
        Colony colony = new Colony();
        colony.id = UUID.randomUUID();
        colony.apiaryId = apiaryId;
        colony.code = code == null || code.isBlank() ? null : code.trim();
        colony.colonyCount = colonyCount == null ? 1 : colonyCount;
        colony.strength = strength == null || strength.isBlank() ? null : strength.trim();
        colony.description = description == null ? "" : description.trim();
        colony.status = "ACTIVE";
        return colony;
    }

    public void update(String strength, String status, String description) {
        if (strength != null) this.strength = strength;
        if (status != null) this.status = status;
        if (description != null) this.description = description.trim();
    }

    @PrePersist
    void onCreate() { if (id == null) id = UUID.randomUUID(); Instant now = Instant.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getApiaryId() { return apiaryId; }
    public String getCode() { return code; }
    public Integer getColonyCount() { return colonyCount; }
    public String getStrength() { return strength; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
