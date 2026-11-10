package uz.melarium.repository;

import java.util.Optional;
import java.util.UUID;

import uz.melarium.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Notification notification set notification.readAt = CURRENT_TIMESTAMP where notification.userId = :userId and notification.readAt is null")
    int markAllRead(@org.springframework.data.repository.query.Param("userId") UUID userId);
}
