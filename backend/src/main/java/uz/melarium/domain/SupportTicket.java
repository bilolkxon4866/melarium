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
@Table(name = "support_tickets")
public class SupportTicket {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false, length = 24) private String category;
    @Column(nullable = false, length = 180) private String subject;
    @Column(nullable = false, columnDefinition = "text") private String message;
    @Column(nullable = false, length = 24) private String status;
    @Column(name = "admin_note", nullable = false, columnDefinition = "text") private String adminNote;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected SupportTicket() { }

    public static SupportTicket create(UUID userId, String category, String subject, String message) {
        SupportTicket ticket = new SupportTicket();
        ticket.id = UUID.randomUUID();
        ticket.userId = userId;
        ticket.category = category;
        ticket.subject = subject;
        ticket.message = message;
        ticket.status = "OPEN";
        ticket.adminNote = "";
        return ticket;
    }

    public void review(String status, String adminNote) {
        this.status = status;
        this.adminNote = adminNote == null ? "" : adminNote;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = "OPEN";
        if (adminNote == null) adminNote = "";
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getCategory() { return category; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public String getAdminNote() { return adminNote; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
