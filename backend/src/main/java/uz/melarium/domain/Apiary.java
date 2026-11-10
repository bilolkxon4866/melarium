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
@Table(name = "apiaries")
public class Apiary {
    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 240)
    private String location;

    @Column(nullable = false, columnDefinition = "text")
    private String description = "";

    @Column(nullable = false, length = 24)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Apiary() { }

    public static Apiary create(UUID ownerId, String name, String location, String description) {
        Apiary apiary = new Apiary();
        apiary.id = UUID.randomUUID();
        apiary.ownerId = ownerId;
        apiary.name = name.trim();
        apiary.location = location.trim();
        apiary.description = description == null ? "" : description.trim();
        apiary.status = "ACTIVE";
        return apiary;
    }

    public void update(String name, String location, String description, String status) {
        if (name != null) this.name = name.trim();
        if (location != null) this.location = location.trim();
        if (description != null) this.description = description.trim();
        if (status != null) this.status = status;
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
    public UUID getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
