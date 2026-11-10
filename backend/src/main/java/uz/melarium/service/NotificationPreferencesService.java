package uz.melarium.service;

import java.util.Map;

import uz.melarium.api.dto.NotificationPreferencesRequest;
import uz.melarium.api.dto.NotificationPreferencesResponse;
import uz.melarium.audit.AuditService;
import uz.melarium.repository.NotificationPreferencesRepository;
import uz.melarium.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPreferencesService {
    private final NotificationPreferencesRepository repository;
    private final AuditService auditService;

    public NotificationPreferencesService(NotificationPreferencesRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional
    public NotificationPreferencesResponse get(AuthenticatedUser actor) {
        return repository.getOrCreate(actor.id());
    }

    @Transactional
    public NotificationPreferencesResponse update(NotificationPreferencesRequest body, AuthenticatedUser actor) {
        NotificationPreferencesResponse saved = repository.update(actor.id(), body.investmentUpdates(), body.reportUpdates(), body.marketingUpdates(), body.pushEnabled());
        auditService.record(actor.id(), "NOTIFICATION_PREFERENCES_UPDATED", "USER", actor.id(), "SUCCESS", Map.of(
                "investmentUpdates", body.investmentUpdates(), "reportUpdates", body.reportUpdates(),
                "marketingUpdates", body.marketingUpdates(), "pushEnabled", body.pushEnabled()));
        return saved;
    }
}
