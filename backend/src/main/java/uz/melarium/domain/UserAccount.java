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
@Table(name = "users")
public class UserAccount {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(name = "full_name", length = 160)
    private String fullName;

    @Column(length = 255)
    private String email;

    @Column(name = "account_status", nullable = false, length = 24)
    private String accountStatus = "PENDING";

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAccount() { }

    public static UserAccount createVerified(String phone) {
        UserAccount user = new UserAccount();
        user.id = UUID.randomUUID();
        user.phone = phone;
        user.accountStatus = "ACTIVE";
        user.phoneVerifiedAt = Instant.now();
        return user;
    }

    public void verifyPhone() {
        this.phoneVerifiedAt = Instant.now();
        this.accountStatus = "ACTIVE";
    }

    public void updateProfile(String fullName, String email) {
        if (fullName != null) this.fullName = fullName.trim();
        if (email != null) this.email = email.isBlank() ? null : email.trim().toLowerCase();
    }

    public void changeAccountStatus(String nextStatus) {
        if (!java.util.Set.of("ACTIVE", "SUSPENDED", "CLOSED").contains(nextStatus)) {
            throw new IllegalArgumentException("Hisob holati noto‘g‘ri");
        }
        this.accountStatus = nextStatus;
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
    public String getPhone() { return phone; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getAccountStatus() { return accountStatus; }
    public Instant getPhoneVerifiedAt() { return phoneVerifiedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
