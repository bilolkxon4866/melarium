package uz.melarium.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "otp_challenges")
public class OtpChallenge {
    @Id
    private UUID id;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "email_hash", length = 64)
    private String emailHash;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "attempt_count", nullable = false)
    private short attemptCount;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "request_ip", length = 64)
    private String requestIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OtpChallenge() { }

    public static OtpChallenge create(String phone, String emailHash, String codeHash, Instant expiresAt, String requestIp) {
        OtpChallenge challenge = new OtpChallenge();
        challenge.id = UUID.randomUUID();
        challenge.phone = phone;
        challenge.emailHash = emailHash;
        challenge.codeHash = codeHash;
        challenge.expiresAt = expiresAt;
        challenge.requestIp = requestIp;
        return challenge;
    }

    public void registerFailedAttempt() { attemptCount++; }
    public void markUsed() { usedAt = Instant.now(); }
    public boolean isExpired(Instant now) { return !expiresAt.isAfter(now); }
    public boolean isLocked() { return attemptCount >= 5; }
    public UUID getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public String getPhone() { return phone; }
    public String getEmailHash() { return emailHash; }
    public String getCodeHash() { return codeHash; }

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
