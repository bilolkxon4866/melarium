package uz.melarium.service;

import java.util.UUID;

import uz.melarium.api.dto.NotificationResponse;
import uz.melarium.domain.Notification;
import uz.melarium.repository.NotificationRepository;
import uz.melarium.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final uz.melarium.repository.NotificationPreferencesRepository preferencesRepository;
    public NotificationService(NotificationRepository notificationRepository, uz.melarium.repository.NotificationPreferencesRepository preferencesRepository) {
        this.notificationRepository = notificationRepository;
        this.preferencesRepository = preferencesRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(AuthenticatedUser actor, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(actor.id(), PageRequest.of(page, size)).map(NotificationResponse::from);
    }

    @Transactional
    public NotificationResponse markRead(UUID id, AuthenticatedUser actor) {
        Notification item = notificationRepository.findByIdAndUserId(id, actor.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bildirishnoma topilmadi"));
        item.markRead();
        return NotificationResponse.from(notificationRepository.save(item));
    }

    @Transactional
    public int markAllRead(AuthenticatedUser actor) {
        return notificationRepository.markAllRead(actor.id());
    }

    @Transactional
    public void create(UUID userId, String title, String body, String kind) {
        if (preferencesRepository.allowsInApp(userId, kind)) {
            notificationRepository.save(Notification.create(userId, title, body, kind));
        }
    }
}
